package com.zuche.module.order.task;

import com.zuche.module.order.service.PickupTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时未取车定时任务
 *
 * <p>默认每天 01:30 执行（排在 01:00 的逾期扫描之后，避免两张任务同时改同一批订单）。
 * cron 可在 {@code application.yml} 的 {@code zuche.order.pickup-timeout-cron} 覆盖。</p>
 *
 * <p>任务只做调度，业务在 {@link PickupTimeoutService#scan()}；
 * 后台「立即扫描」按钮调用的也是同一个方法，保证定时与手动口径一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickupTimeoutTask {

    private final PickupTimeoutService pickupTimeoutService;

    @Scheduled(cron = "${zuche.order.pickup-timeout-cron:0 30 1 * * ?}")
    public void scan() {
        try {
            pickupTimeoutService.scan();
        } catch (Exception e) {
            log.error("超时未取车任务执行失败", e);
        }
    }
}
