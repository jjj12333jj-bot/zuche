package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细项
 */
@Data
@Schema(description = "订单明细项")
public class OrderItemVO {

    private Long id;

    private Long vehicleId;

    private String vehicleName;

    private String licensePlate;

    @Schema(description = "快照日租金")
    private BigDecimal dailyPrice;

    private Integer quantity;

    private Integer rentDays;

    private BigDecimal discountRate;

    @Schema(description = "本项租金 = 日租金 × 天数 × 数量 × 折扣")
    private BigDecimal rentAmount;

    @Schema(description = "本项押金 = 标准押金 × 信用系数 × 数量")
    private BigDecimal depositAmount;
}
