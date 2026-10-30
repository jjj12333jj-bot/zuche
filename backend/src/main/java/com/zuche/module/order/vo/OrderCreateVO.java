package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单结果：只回订单号与应付金额，前端据此跳收银台
 */
@Data
@Schema(description = "下单结果")
public class OrderCreateVO {

    private Long orderId;

    private String orderNo;

    private String status;

    private String statusLabel;

    private BigDecimal rentAmount;

    private BigDecimal depositAmount;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;
}
