package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台订单统计（全量，不受分页影响）
 */
@Data
@Schema(description = "平台订单统计")
public class AdminOrderStatsVO {

    @Schema(description = "订单总数")
    private Integer total;

    @Schema(description = "待支付")
    private Integer pendingPay;

    @Schema(description = "待取车")
    private Integer pendingPickup;

    @Schema(description = "在租中（已取车 + 待归还 + 逾期）")
    private Integer inUse;

    @Schema(description = "逾期未还")
    private Integer overdue;

    @Schema(description = "今日新增订单")
    private Integer todayNew;

    @Schema(description = "逾期违约金合计（仅已结算部分）")
    private BigDecimal lateFeeTotal;
}
