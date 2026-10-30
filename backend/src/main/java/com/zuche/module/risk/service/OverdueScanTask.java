package com.zuche.module.risk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 逾期扫描定时任务
 *
 * <p>默认每天凌晨 1:00 执行（业务低峰，避免和用户归还抢行锁），
 * cron 可在 {@code application.yml} 的 {@code zuche.risk.scan-cron} 覆盖。</p>
 *
 * <p>任务本身只做"调度"，业务逻辑全在 {@link RiskControlService#scanOverdue()}，
 * 后台的"立即扫描"按钮调用的是同一个方法——保证定时执行和手动执行口径完全一致，
 * 不会出现"定时任务和接口逻辑各写一套、结果不一样"的经典问题。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueScanTask {

    private final RiskControlService riskControlService;

    @Scheduled(cron = "${zuche.risk.scan-cron:0 0 1 * * ?}")
    public void scanOverdue() {
        try {
            riskControlService.scanOverdue();
        } catch (Exception e) {
            // 定时任务里抛异常会让后续调度继续跑，但日志必须留全，否则线上无从排查
            log.error("逾期扫描任务执行失败", e);
        }
    }
}
