package com.zuche.module.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车型偏好矩阵的一个点：一个车型分类
 *
 * <p>用「订单量 × 客单价 × 营收规模」三个维度描述偏好结构，
 * 单看订单排行看不出"豪华型单少但客单高"这类结论。</p>
 */
@Data
@Schema(description = "车型分类偏好")
public class ModelPreferenceVO {

    @Schema(description = "分类字典码，如 ECONOMY")
    private String categoryCode;

    @Schema(description = "分类中文名")
    private String categoryLabel;

    @Schema(description = "分类主题色（取自数据字典）")
    private String color;

    @Schema(description = "订单量")
    private Integer orderCount;

    @Schema(description = "订单量占比（%）")
    private Double orderRatio;

    @Schema(description = "租金实收")
    private BigDecimal rentAmount;

    @Schema(description = "营收占比（%）")
    private Double rentRatio;

    @Schema(description = "客单价（元/单）")
    private BigDecimal avgOrderAmount;

    @Schema(description = "平均租期（天）")
    private BigDecimal avgRentDays;

    @Schema(description = "平均日租金")
    private BigDecimal avgDailyPrice;

    @Schema(description = "该分类在册车辆数")
    private Integer vehicleCount;

    @Schema(description = "单车单周产出（租金 ÷ 车辆数 ÷ 周数）")
    private BigDecimal rentPerVehicleWeek;
}
