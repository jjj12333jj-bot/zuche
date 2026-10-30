package com.zuche.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁订单（对应 lease_order）
 *
 * <p>信用快照三字段（score/level/coefficient）在下单时写入，之后不再跟随用户当前分数变化，
 * 保证"当时按什么系数收的押金"可追溯、可对账。</p>
 */
@Data
@TableName("lease_order")
@Schema(description = "租赁订单")
public class LeaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 取车网点 */
    private Long storeId;

    /** 归还网点（支持异地还车，为空表示原网点归还） */
    private Long returnStoreId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer rentDays;

    /** 车辆数量（= 明细条数） */
    private Integer vehicleCount;

    private BigDecimal rentAmount;

    private BigDecimal depositAmount;

    private BigDecimal totalAmount;

    /* ===== 信用快照 ===== */

    private Integer creditScoreSnapshot;

    private String creditLevelSnapshot;

    private BigDecimal depositCoefficient;

    /** WAIT_PAY / WAIT_PICKUP / IN_USE / WAIT_RETURN / RETURNED / COMPLETED / CANCELLED / OVERDUE */
    private String status;

    private LocalDateTime payTime;

    private LocalDateTime pickupTime;

    private LocalDateTime returnApplyTime;

    private LocalDateTime returnTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private Integer overdueDays;

    private BigDecimal lateFee;

    /** NONE / PENDING / REFUNDED */
    private String depositRefundStatus;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
