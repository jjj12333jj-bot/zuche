package com.zuche.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机
 *
 * <p>流转规则集中定义在此，Service 层每次变更状态前调用 {@link #canTransfer}，
 * 非法流转直接抛业务异常，避免"状态被随手改"。</p>
 *
 * <pre>
 * WAIT_PAY ──支付──> WAIT_PICKUP ──交付/取车──> IN_USE ──申请归还──> WAIT_RETURN ──确认归还──> RETURNED ──押金结算──> COMPLETED
 *     │                    │                        │                        │
 *     └──取消──> CANCELLED └──取消──> CANCELLED     └──超期(定时任务)──> OVERDUE ──归还──> WAIT_RETURN/RETURNED
 *                                              WAIT_RETURN ──超期(定时任务)──> OVERDUE
 * </pre>
 *
 * <p>{@code WAIT_RETURN → OVERDUE} 是 S4 新增的：用户提交了归还申请却一直没到店确认，
 * 车实际上仍然没还回来，不能因为点了一个按钮就免除逾期责任。</p>
 */
@Getter
public enum OrderStatus {

    WAIT_PAY("待支付"),
    WAIT_PICKUP("待取车"),
    IN_USE("已取车/在租"),
    WAIT_RETURN("待归还"),
    RETURNED("已归还"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    OVERDUE("逾期");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** 允许的目标状态 */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            WAIT_PAY, Set.of(WAIT_PICKUP, CANCELLED),
            WAIT_PICKUP, Set.of(IN_USE, CANCELLED),
            IN_USE, Set.of(WAIT_RETURN, OVERDUE),
            WAIT_RETURN, Set.of(RETURNED, OVERDUE),
            OVERDUE, Set.of(WAIT_RETURN, RETURNED),
            RETURNED, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransfer(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static OrderStatus of(String code) {
        return Arrays.stream(values())
                .filter(s -> s.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知订单状态: " + code));
    }
}
