package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 网点经营统计
 *
 * <p>{@code perVehicle} 是"单车产出"，用租金 / 车辆数；
 * {@code utilizationRate} 是"车日利用率"，用占用车日 / 可租车日。
 * 后者是时间加权口径，要求种子数据里同一台车不出现档期重叠 —— 02-seed.sql
 * 的排程过程保证了这一点，所以这里的 SUM 不会被重复计数放大。</p>
 */
@Data
@Schema(description = "网点经营统计")
public class StoreStatVO {

    @Schema(description = "网点 ID")
    private Long storeId;

    @Schema(description = "网点名称")
    private String storeName;

    @Schema(description = "在册车辆数")
    private Integer vehicleCount;

    @Schema(description = "可租车辆数（排除下架与维保）")
    private Integer rentableVehicles;

    @Schema(description = "窗口内被占用车日合计")
    private Integer occupiedVehicleDays;

    @Schema(description = "车日利用率（%）")
    private Double utilizationRate;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "租金实收")
    private BigDecimal rentAmount;

    @Schema(description = "单车产出")
    private BigDecimal perVehicle;

    @Schema(description = "逾期订单数")
    private Integer overdueCount;

    @Schema(description = "已完成订单数")
    private Integer completedCount;

    @Schema(description = "订单完成率（%）")
    private Double completeRate;
}
