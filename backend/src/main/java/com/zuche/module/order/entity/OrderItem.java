package com.zuche.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细（对应 order_item）
 *
 * <p>一单可租多台车，每台车一行。日租金、折扣、押金系数全部快照，
 * 车辆后续调价或用户信用变化都不影响历史订单金额。</p>
 */
@Data
@TableName("order_item")
@Schema(description = "订单明细")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long vehicleId;

    /** 快照：品牌 + 车型 */
    private String vehicleName;

    private String licensePlate;

    private BigDecimal dailyPrice;

    private Integer quantity;

    private Integer rentDays;

    private BigDecimal discountRate;

    /** 日租金 × 天数 × 数量 × 折扣 */
    private BigDecimal rentAmount;

    /** 标准押金 × 信用系数 × 数量 */
    private BigDecimal depositAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
