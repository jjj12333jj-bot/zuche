package com.zuche.module.credit.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zuche.module.credit.mapper.CreditLogMapper;
import com.zuche.module.user.entity.SysUser;
import com.zuche.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 行为分基线校准（启动时执行一次，幂等）
 *
 * <p><b>解决什么问题：</b>行为分是在订单进入终态（完成/取消/归还）时重算的。
 * 但库里已有的老用户从来没有走过新流程，他们的 credit_log 里没有 BEHAVIOR 记录，
 * 于是出现"信用中心显示行为分 0，但按他的真实履约特征应该是 -15"的错位——
 * 分数虚高，风控形同虚设。</p>
 *
 * <p><b>怎么做：</b>启动时扫一遍用户，凡是还没做过基线校准的，按当前行为特征补一次差额。
 * 校准结果照样写 credit_log（change_type = BEHAVIOR），所以分数恒等式不受影响，
 * 明细里也能查到"这次为什么掉了 15 分"。</p>
 *
 * <p><b>为什么安全：</b>已有 BEHAVIOR 记录的用户直接跳过；行为分目标值本来就是 0 的用户
 * 不会产生任何写入。重复启动最多是几次只读查询，不会重复扣分。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditBaselineRunner implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final CreditLogMapper creditLogMapper;
    private final CreditService creditService;

    @Override
    public void run(ApplicationArguments args) {
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId));

        int calibrated = 0;
        for (SysUser user : users) {
            try {
                if (creditLogMapper.countBehaviorLog(user.getId()) > 0) {
                    continue;
                }
                if (creditService.refitBehavior(user.getId(), null) != null) {
                    calibrated++;
                }
            } catch (Exception e) {
                // 单个用户算不出来不能拖垮整个应用启动
                log.warn("行为分基线校准失败: userId={}, {}", user.getId(), e.getMessage());
            }
        }

        if (calibrated > 0) {
            log.info("行为分基线校准完成：本次为 {} 个存量用户补算了行为分", calibrated);
        }
    }
}
