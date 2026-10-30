package com.zuche.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuche.common.Result;
import com.zuche.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 角色权限拦截器（在 JwtInterceptor 之后执行）
 *
 * <p>只对标注了 {@link RequireRole} 的方法/类生效；未标注的接口仅要求登录。
 * 这是后端唯一的权限边界，前端菜单隐藏只是体验优化。</p>
 */
@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireRole require = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (require == null) {
            require = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (require == null) {
            return true;
        }

        LoginUser user = UserContext.get();
        if (user == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.UNAUTHORIZED, "请先登录");
            return false;
        }
        boolean allowed = Arrays.asList(require.value()).contains(user.getRoleCode());
        if (!allowed) {
            write(response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN, "无权访问该接口");
            return false;
        }
        return true;
    }

    private void write(HttpServletResponse response, int httpStatus, ResultCode code, String message)
            throws Exception {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }
}
