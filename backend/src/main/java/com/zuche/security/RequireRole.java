package com.zuche.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色权限注解
 *
 * <p>用法：{@code @RequireRole(RoleCode.SYS_ADMIN)}，可标在方法或类上（类上作为默认）。
 * 由 {@link RoleInterceptor} 在 JWT 解析之后校验，角色不符返回 403。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色码，满足其一即可 */
    String[] value();
}
