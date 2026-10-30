package com.zuche.module.store.service;

import com.zuche.common.BusinessException;
import com.zuche.common.ResultCode;
import com.zuche.security.LoginUser;
import com.zuche.security.UserContext;

/**
 * 网点上下文
 *
 * <p>三个网点 Service 都要做同一件事：确认当前登录人是网点管理员并拿到 storeId。
 * 抽出来只有一个目的——「我是哪个网点」这个判断永远只有一处实现，
 * 将来要支持一个管理员管多个网点（store_id 变多值），改这里就行。</p>
 */
public final class StoreContext {

    private StoreContext() {
    }

    /** 当前登录的网点管理员；非网点管理员直接 403 */
    public static LoginUser require() {
        LoginUser login = UserContext.get();
        if (login == null || !login.isStoreAdmin() || login.getStoreId() == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅网点管理员可访问该功能");
        }
        return login;
    }

    public static Long requireStoreId() {
        return require().getStoreId();
    }

    public static Long requireUserId() {
        return require().getUserId();
    }
}
