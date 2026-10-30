package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户租赁行为统计（行为分模型的输入）
 *
 * <p>全部由 lease_order 现算，不落冗余字段：数据量是"一个用户几十单"级别，
 * 现算比维护冗余列更不容易出错。</p>
 */
@Data
@Schema(description = "信用行为统计")
public class CreditBehaviorStats {

    @Schema(description = "统计窗口：最近多少笔已支付订单")
    private Integer windowSize;

    @Schema(description = "已支付订单数（含后续取消/完成的）")
    private Integer paidCount;

    @Schema(description = "支付后取消的订单数")
    private Integer paidCancelCount;

    @Schema(description = "已完成订单数")
    private Integer finishedCount;

    @Schema(description = "按时（未逾期）完成的订单数")
    private Integer onTimeCount;

    @Schema(description = "发生过逾期的订单数")
    private Integer overdueCount;

    @Schema(description = "按时归还率 0~1")
    private BigDecimal onTimeRate;

    @Schema(description = "支付后取消率 0~1")
    private BigDecimal cancelRate;
}
