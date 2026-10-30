package com.zuche.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水（对应 payment_record，模拟支付）
 *
 * <p>一笔订单支付成功会生成两条流水：RENT（租金）+ DEPOSIT（押金）。
 * 这样营收口径只统计 RENT，押金始终是"代收待退"的负债，不会被算成收入。</p>
 */
@Data
@TableName("payment_record")
@Schema(description = "支付流水")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private Long orderId;

    private Long userId;

    /** RENT 租金 / DEPOSIT 押金 / LATE_FEE 违约金 */
    private String payType;

    private BigDecimal amount;

    /** MOCK_WECHAT / MOCK_ALIPAY */
    private String payMethod;

    /** PENDING / SUCCESS / FAILED */
    private String status;

    private LocalDateTime payTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
