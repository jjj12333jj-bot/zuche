package com.zuche.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuche.common.Result;
import com.zuche.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器
 *
 * <p>1. 白名单接口在 WebMvcConfig 中 exclude，不进这里；
 * 2. 解析成功写入 UserContext，失败返回 401 JSON（HTTP 状态与业务码同时为 401）；
 * 3. afterCompletion 必须清理 ThreadLocal，否则线程复用会串用户。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        String token = header.startsWith(BEARER) ? header.substring(BEARER.length()).trim() : header.trim();
        try {
            UserContext.set(jwtUtil.parse(token));
            return true;
        } catch (Exception e) {
            log.debug("token 解析失败: {}", e.getMessage());
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.fail(ResultCode.UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
