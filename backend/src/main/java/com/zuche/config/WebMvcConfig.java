package com.zuche.config;

import com.zuche.security.JwtInterceptor;
import com.zuche.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：跨域 + 鉴权/权限拦截器注册
 *
 * <p>白名单只放：登录注册、健康检查、以及后续的车辆/首页/公告等公开浏览接口、
 * 接口文档静态资源。其余一律要求登录。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RoleInterceptor roleInterceptor;

    /** 无需登录即可访问的路径 */
    public static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/system/health",
            "/api/home",
            "/api/categories/**",
            "/api/vehicles/**",
            "/api/announcements/**",
            // 接口文档与静态资源
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/favicon.ico",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 顺序很重要：先解析 token，再校验角色
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(WHITE_LIST)
                .order(1);
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/**")
                .order(2);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
