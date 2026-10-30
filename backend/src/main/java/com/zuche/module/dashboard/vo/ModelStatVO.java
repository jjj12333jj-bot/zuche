package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车型经营统计（营收排行用）
 */
@Data
@Schema(description = "车型经营统计")
public class ModelStatVO {

    @Schema(description = "车型名称")
    private String modelName;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "租金实收")
    private BigDecimal rentAmount;

    @Schema(description = "平均租期（天）")
    private BigDecimal avgRentDays;
}
