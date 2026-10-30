package com.zuche.module.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuche.module.system.annotation.OpLog;
import com.zuche.module.system.entity.OperationLog;
import com.zuche.module.system.service.OperationLogService;
import com.zuche.security.LoginUser;
import com.zuche.security.UserContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 *
 * <p>为什么切在 Controller 而不是 Service：Service 方法上大多有 {@code @Transactional}，
 * 切面如果套在事务里面，一旦业务回滚，刚写的日志也被一起回滚——最该留痕的失败操作反而没了记录。
 * 切在 Controller 外层，事务已在 Service 返回时提交，日志是一次独立的写入。</p>
 *
 * <p>日志写入失败只打 warn、不抛异常：审计不能反过来把业务搞挂。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 参数快照上限，超长截断——一条日志塞进几 MB 的正文没有意义 */
    private static final int PARAMS_MAX = 2000;

    /** 敏感字段脱敏：password / token / idCard 等一律替换为 ****** */
    private static final Pattern SENSITIVE = Pattern.compile(
            "(\"[^\"]*(?:password|passwd|token|secret|idcard|bankcard)[^\"]*\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            Pattern.CASE_INSENSITIVE);

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                operationLogService.record(build(joinPoint, opLog, cost, success));
            } catch (Exception e) {
                log.warn("操作日志组装失败: module={}, operation={}, err={}",
                        opLog.module(), opLog.operation(), e.getMessage());
            }
        }
    }

    private OperationLog build(ProceedingJoinPoint joinPoint, OpLog opLog, long cost, boolean success) {
        OperationLog entry = new OperationLog();
        entry.setModule(opLog.module());
        entry.setOperation(opLog.operation());
        entry.setCostMs(cost);
        entry.setStatus(success ? 1 : 0);
        entry.setCreateTime(LocalDateTime.now());
        entry.setUpdateTime(LocalDateTime.now());

        LoginUser user = UserContext.get();
        if (user != null) {
            entry.setUserId(user.getUserId());
            entry.setUsername(user.getUsername());
        }

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entry.setMethod(request.getMethod());
            entry.setRequestUri(truncate(request.getRequestURI(), 200));
            entry.setIp(clientIp(request));
        }
        if (opLog.saveParams()) {
            entry.setParams(serializeArgs(joinPoint));
        }
        return entry;
    }

    private HttpServletRequest currentRequest() {
        // 定时任务、异步线程里没有请求上下文，此时只记「谁 + 做了什么」
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        // 编译期开了 -parameters，这里能拿到真实的形参名（id / dto），而不是 arg0
        String[] names = joinPoint.getSignature() instanceof MethodSignature ms ? ms.getParameterNames() : null;

        List<String> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null
                    || arg instanceof ServletRequest
                    || arg instanceof ServletResponse
                    || arg instanceof BindingResult
                    || arg instanceof MultipartFile) {
                continue;
            }
            keys.add(names != null && i < names.length ? names[i] : "arg" + i);
            values.add(arg);
        }
        if (values.isEmpty()) {
            return null;
        }
        try {
            String json;
            if (values.size() == 1) {
                Object only = values.get(0);
                // 单个简单参数（如 @PathVariable Long id）包成 {"id":8}，比光秃秃一个 8 好读
                json = isSimple(only)
                        ? objectMapper.writeValueAsString(Map.of(keys.get(0), only))
                        : objectMapper.writeValueAsString(only);
            } else {
                Map<String, Object> payload = new LinkedHashMap<>();
                for (int i = 0; i < values.size(); i++) {
                    payload.put(keys.get(i), values.get(i));
                }
                json = objectMapper.writeValueAsString(payload);
            }
            return truncate(SENSITIVE.matcher(json).replaceAll("$1\"******\""), PARAMS_MAX);
        } catch (Exception e) {
            // 参数里有无法序列化的东西（如流对象）时，不要让日志拖累业务
            return "[参数无法序列化: " + e.getClass().getSimpleName() + "]";
        }
    }

    private static boolean isSimple(Object value) {
        return value instanceof Number
                || value instanceof CharSequence
                || value instanceof Boolean
                || value instanceof Character;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return truncate(comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim(), 50);
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return truncate(real.trim(), 50);
        }
        return truncate(request.getRemoteAddr(), 50);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
