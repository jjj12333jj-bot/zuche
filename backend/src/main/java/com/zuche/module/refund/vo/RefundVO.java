package com.zuche.module.refund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水展示项
 */
@Data
@Schema(description = "退款流水")
public class RefundVO {

    private Long id;

    private String refundNo;

    private Long paymentId;

    private String refundType;

    @Schema(description = "类型中文名")
    private String refundTypeLabel;

    private BigDecimal amount;

    private String reason;

    private String status;

    @Schema(description = "状态中文名")
    private String statusLabel;

    private LocalDateTime refundTime;
}
