package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 网点经营统计
 *
 * <p>{@code perVehicle} 是"单车产出"，用租金 / 车辆数，
 * 比"出租率"更能反映网点的经营质量 —— 本项目种子数据存在同车重叠订单，
 * 车日利用率会算到 500% 以上，展示出来不可信，所以改用这个口径。</p>
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
