package com.zuche.security;

/**
 * 当前请求的登录用户（ThreadLocal）
 *
 * <p>由 JwtInterceptor 在 preHandle 写入、afterCompletion 清理，
 * 业务代码通过 UserContext.getUserId() 取当前用户，避免层层传参。</p>
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static Long requireUserId() {
        Long id = getUserId();
        if (id == null) {
            throw new com.zuche.common.BusinessException(com.zuche.common.ResultCode.UNAUTHORIZED);
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
