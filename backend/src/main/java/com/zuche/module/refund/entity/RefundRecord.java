package com.zuche.module.refund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水（对应 refund_record，模拟退款）
 *
 * <p>与支付流水分开建表，而不是在 payment_record 上打一个"退款"标记：
 * 一笔支付可能分多次退（比如押金先退、违约金后扣），一条支付对应多条退款，
 * 分表才能保证每次退款都有独立凭证号。</p>
 */
@Data
@TableName("refund_record")
@Schema(description = "退款流水")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long orderId;

    /** 原支付流水 ID：押金退还时指向 DEPOSIT 那条 */
    private Long paymentId;

    private Long userId;

    /** DEPOSIT 押金退还 / CANCEL 取消退款 / LATE_FEE 违约金冲正 */
    private String refundType;

    private BigDecimal amount;

    private String reason;

    /** PENDING / REFUNDED / FAILED */
    private String status;

    private LocalDateTime applyTime;

    private LocalDateTime refundTime;

    private String channel;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
