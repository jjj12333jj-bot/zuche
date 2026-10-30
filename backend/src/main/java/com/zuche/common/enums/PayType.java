package com.zuche.common.enums;

import lombok.Getter;

/**
 * 支付/退款流水类型
 *
 * <p>一笔订单支付时生成 RENT + DEPOSIT 两条流水：
 * 营收口径只统计 RENT，押金减免金额才能被精确衡量；退押金是独立的 REFUND_DEPOSIT 流水。</p>
 */
@Getter
public enum PayType {

    RENT("租金"),
    DEPOSIT("押金"),
    LATE_FEE("违约金"),
    REFUND_DEPOSIT("退还押金");

    private final String label;

    PayType(String label) {
        this.label = label;
    }
}
