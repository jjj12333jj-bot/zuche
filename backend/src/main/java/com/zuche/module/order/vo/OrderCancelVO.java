package com.zuche.module.order.vo;

import com.zuche.module.credit.vo.CreditChangeVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 取消订单结果
 *
 * <p>把"退了多少钱""扣了几分"直接带回前端，用户点完取消立刻看到结果，
 * 不用再刷新两次页面才知道信用分变了。</p>
 */
@Data
@Schema(description = "取消订单结果")
public class OrderCancelVO {

    private Long orderId;

    private String orderNo;

    private String status;

    private String statusLabel;

    @Schema(description = "本次退款合计（未支付订单为 0）")
    private BigDecimal refundAmount;

    @Schema(description = "信用变动，未支付订单为空")
    private CreditChangeVO creditChange;

    @Schema(description = "行为分重算结果，无变化时为空")
    private CreditChangeVO behaviorChange;
}
