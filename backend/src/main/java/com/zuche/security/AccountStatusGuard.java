package com.zuche.security;

import com.zuche.module.user.entity.SysUser;
import com.zuche.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号状态守卫：让「冻结」当场生效
 *
 * <p>问题：JWT 是无状态的，签出去就收不回来。后台把用户冻结后，
 * 他手里的旧 token 仍然能用到过期为止（默认 7 天），等于冻结没生效。</p>
 *
 * <p>做法：拦截器每次请求过一下这里。为了不把每个 API 都变成一次数据库查询，
 * 结果按 userId 缓存 60 秒；冻结/解冻时由业务侧主动 {@link #evict}，
 * 同一实例上立刻失效，其他实例最多延迟 60 秒。</p>
 *
 * <p>为什么不把 status 写进 JWT：写进去就固化了，冻结要等 token 过期，
 * 跟不做的效果一样。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountStatusGuard {

    /** 缓存有效期：60 秒。既是性能折中，也是多实例下冻结生效的最大延迟 */
    private static final long TTL_MS = 60_000L;

    private final SysUserMapper userMapper;

    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    /**
     * 账号是否不可用（已冻结或已注销）
     *
     * <p>查不到用户也按不可用处理：逻辑删除的账号不该继续持有有效会话。</p>
     */
    public boolean isDisabled(Long userId) {
        if (userId == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        Entry cached = cache.get(userId);
        if (cached != null && now - cached.at() < TTL_MS) {
            return cached.disabled();
        }
        SysUser user = userMapper.selectById(userId);
        boolean disabled = user == null || Integer.valueOf(0).equals(user.getStatus());
        cache.put(userId, new Entry(disabled, now));
        if (disabled) {
            log.debug("账号不可用: userId={}, 存在={}", userId, user != null);
        }
        return disabled;
    }

    /** 状态变更后主动失效，避免最长 60 秒的窗口期 */
    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private record Entry(boolean disabled, long at) {
    }
}
