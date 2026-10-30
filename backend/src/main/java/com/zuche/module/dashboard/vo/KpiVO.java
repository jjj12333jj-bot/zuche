package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运营看板顶部指标卡
 *
 * <p>口径说明（答辩时会被问，先写清楚）：</p>
 * <ul>
 *   <li><b>净营收</b> = 租金实收 − 租金退款 + 违约金。押金是代收代退，不计入营收。</li>
 *   <li><b>平均客单价</b> = 租金实收 / 已支付订单数，不含押金。</li>
 *   <li><b>出租率</b> = 当前已租车辆 / 在册车辆，是<b>瞬时快照</b>，不是时间加权利用率。</li>
 *   <li><b>车日利用率</b> = 窗口内被占用车日 / 可租车日，是时间加权口径。
 *       种子数据保证同一台车不出现档期重叠，所以 SUM 不会重复计数。</li>
 * </ul>
 */
@Data
@Schema(description = "运营看板指标卡")
public class KpiVO {

    @Schema(description = "净营收（租金实收 - 租金退款 + 违约金）")
    private BigDecimal revenue;

    @Schema(description = "同口径下所选时间范围内的营收")
    private BigDecimal rangeRevenue;

    @Schema(description = "违约金合计")
    private BigDecimal lateFeeTotal;

    @Schema(description = "平均客单价（不含押金）")
    private BigDecimal avgOrderAmount;

    @Schema(description = "订单总量")
    private Integer orderTotal;

    @Schema(description = "已支付订单数")
    private Integer paidOrders;

    @Schema(description = "已完成订单数")
    private Integer completedOrders;

    @Schema(description = "已取消订单数")
    private Integer cancelledOrders;

    @Schema(description = "累计逾期订单数")
    private Integer overdueOrders;

    @Schema(description = "订单完成率（%）")
    private Double completeRate;

    @Schema(description = "在册车辆数")
    private Integer vehicleTotal;

    @Schema(description = "当前已租车辆数")
    private Integer vehicleRented;

    @Schema(description = "车辆出租率（%）")
    private Double rentRate;

    @Schema(description = "可租车辆数（排除下架与维保）")
    private Integer rentableVehicles;

    @Schema(description = "窗口内被占用车日合计")
    private Integer occupiedVehicleDays;

    @Schema(description = "车日利用率（%）= 占用车日 / (可租车辆 × 统计天数)")
    private Double utilizationRate;

    @Schema(description = "车日利用率的实际统计天数（不超过数据覆盖天数）")
    private Integer utilizationDays;

    @Schema(description = "用户总数（仅 USER 角色）")
    private Integer userTotal;
}
