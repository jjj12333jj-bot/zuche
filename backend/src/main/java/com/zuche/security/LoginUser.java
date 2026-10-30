package com.zuche.security;

import lombok.Data;

/**
 * 登录用户上下文对象（从 JWT 解析而来，不含密码等敏感字段）
 */
@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String roleCode;
    /** 网点管理员所属网点 */
    private Long storeId;

    public boolean isAdmin() {
        return RoleCode.SYS_ADMIN.equals(roleCode);
    }

    public boolean isStoreAdmin() {
        return RoleCode.STORE_ADMIN.equals(roleCode);
    }

    public boolean isUser() {
        return RoleCode.USER.equals(roleCode);
    }
}
