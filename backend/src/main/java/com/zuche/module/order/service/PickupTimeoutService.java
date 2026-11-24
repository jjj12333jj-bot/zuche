package com.zuche.module.order.service;

import com.zuche.module.order.entity.LeaseOrder;
import com.zuche.module.order.mapper.LeaseOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 超时未取车扫描
 *
 * <p>为什么要这个：用户付完钱一直不来取车，车就一直被这单占着档期——别人租不了，
 * 商户也不敢手动取消（取消要退款、要扣分，得有人负责）。这一段把"过期未取车"
 * 变成一条自动规则：超过 {@code zuche.order.pickup-timeout-days} 天还没取车，
 * 系统按"支付后取消"的口径处理——退款、释放车辆、扣信用分、写日志。</p>
 *
 * <p>逐单独立事务：一单失败不影响其他单，失败的那单下次扫描还会重试。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickupTimeoutService {

    private final LeaseOrderMapper orderMapper;
    private final OrderService orderService;

    /** 允许的最晚取车宽限天数：取车日之后第 N 天仍未取车即判定超时 */
    @Value("${zuche.order.pickup-timeout-days:1}")
    private int timeoutDays;

    /**
     * 扫描并处理超时未取车的订单
     *
     * @return 本次成功取消的订单号列表（用于接口返回与日志核对）
     */
    public List<String> scan() {
        LocalDate deadline = LocalDate.now().minusDays(Math.max(1, timeoutDays));
        List<LeaseOrder> candidates = orderMapper.selectPickupTimeoutCandidates(deadline);
        List<String> cancelled = new ArrayList<>();
        if (candidates.isEmpty()) {
            log.info("超时未取车扫描完成: 无候选（截止日 {}）", deadline);
            return cancelled;
        }

        String reason = "超过取车日 " + Math.max(1, timeoutDays) + " 天未取车，系统自动取消";
        for (LeaseOrder order : candidates) {
            try {
                if (orderService.cancelTimeoutPickup(order.getId(), reason)) {
                    cancelled.add(order.getOrderNo());
                }
            } catch (Exception e) {
                // 单笔失败（并发取车、退款异常）不影响后续订单，留日志下次再扫
                log.warn("超时未取车取消失败: orderNo={}", order.getOrderNo(), e);
            }
        }
        log.info("超时未取车扫描完成: 候选 {} 单, 实际取消 {} 单, 截止日 {}",
                candidates.size(), cancelled.size(), deadline);
        return cancelled;
    }
}
