package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模拟支付结果
 *
 * <p>返回 payments 数组，前端可直接把两条流水（租金 / 押金）展示出来，
 * 说明"押金不是收入、是代收待退"，这是答辩时容易被追问的点。</p>
 */
@Data
@Schema(description = "模拟支付结果")
public class OrderPayVO {

    private Long orderId;

    private String orderNo;

    private String status;

    private String statusLabel;

    private LocalDateTime payTime;

    @Schema(description = "本次实付合计")
    private BigDecimal paidAmount;

    @Schema(description = "本次生成的流水（RENT + DEPOSIT）")
    private List<PaymentVO> payments;
}
