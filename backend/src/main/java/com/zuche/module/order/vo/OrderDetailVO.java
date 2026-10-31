package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情：主表 + 明细 + 流水 + 信用快照
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单详情")
public class OrderDetailVO extends OrderListVO {

    private Long returnStoreId;

    private String returnStoreName;

    private String remark;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime pickupTime;

    private LocalDateTime returnApplyTime;

    private LocalDateTime returnTime;

    private LocalDateTime finishTime;

    private Integer overdueDays;

    @Schema(description = "违约金：未结算时是按当前规则算出的预估，结算后是实际扣款")
    private BigDecimal lateFee;

    @Schema(description = "违约金是否已结算（true=已从押金扣除；false=预估，归还时才扣）")
    private Boolean lateFeeSettled;

    @Schema(description = "押金退还状态 NONE/PENDING/REFUNDED")
    private String depositRefundStatus;

    @Schema(description = "下单时信用分快照")
    private Integer creditScoreSnapshot;

    @Schema(description = "标准押金合计（未乘系数）")
    private BigDecimal depositStandardTotal;

    @Schema(description = "信用减免金额")
    private BigDecimal depositSaved;

    @Schema(description = "订单明细")
    private List<OrderItemVO> items;

    @Schema(description = "支付流水")
    private List<PaymentVO> payments;

    @Schema(description = "退款流水")
    private List<com.zuche.module.refund.vo.RefundVO> refunds;

    @Schema(description = "本人对这笔订单的评价，未评价为 null")
    private EvaluationVO evaluation;
}
