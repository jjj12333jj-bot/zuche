package com.zuche.module.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 网点概览
 *
 * <p>全部指标限定在「本网点」，是网点管理员登录后的第一屏：
 * 上面是资产与订单的实时存量，下面是今日/本月的经营量。</p>
 */
@Data
@Schema(description = "网点概览")
public class StoreOverviewVO {

    private Long storeId;

    private String storeName;

    private String address;

    private String phone;

    private String businessHours;

    /* ==================== 车辆资产 ==================== */

    @Schema(description = "在册车辆数")
    private Integer vehicleTotal;

    @Schema(description = "可租（status=1）")
    private Integer vehicleAvailable;

    @Schema(description = "已租出（status=2）")
    private Integer vehicleRented;

    @Schema(description = "维保中（status=3）")
    private Integer vehicleMaintenance;

    @Schema(description = "出租率 = 已租出 / 在册，瞬时快照")
    private Double rentRate;

    @Schema(description = "可租车辆数（排除下架与维保）")
    private Integer rentableVehicles;

    @Schema(description = "近 30 天被占用车日")
    private Integer occupiedVehicleDays;

    @Schema(description = "近 30 天车日利用率（%）：占用车日 ÷ (可租车辆 × 30)")
    private Double utilizationRate;

    /* ==================== 订单存量（当前状态） ==================== */

    @Schema(description = "待取车：已付款等待交付")
    private Integer pendingPickup;

    @Schema(description = "在租：已交付未归还")
    private Integer inUse;

    @Schema(description = "待归还：用户已申请，等待网点验收")
    private Integer pendingReturn;

    @Schema(description = "逾期：已过计划还车日仍未归还")
    private Integer overdue;

    @Schema(description = "待办合计 = 待取车 + 待归还 + 逾期")
    private Integer todoCount;

    /* ==================== 今日 / 本月经营 ==================== */

    @Schema(description = "今日新下单数")
    private Integer todayNewOrders;

    @Schema(description = "今日完成归还数")
    private Integer todayCompleted;

    @Schema(description = "本月新下单数")
    private Integer monthOrders;

    @Schema(description = "本月租金实收（不含押金）")
    private BigDecimal monthRevenue;

    @Schema(description = "在管押金 = 未完成订单的押金合计，属于代收代退")
    private BigDecimal depositInHand;
}
