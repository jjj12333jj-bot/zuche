package com.zuche.module.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 热力图的一个格子：某星期的某个小时
 */
@Data
@Schema(description = "下单热力格子")
public class HeatmapCellVO {

    @Schema(description = "星期，1=周一 … 7=周日")
    private Integer weekday;

    @Schema(description = "小时，0-23")
    private Integer hour;

    @Schema(description = "该格子内的下单量")
    private Integer orderCount;

    @Schema(description = "该格子内的租金合计")
    private BigDecimal rentAmount;
}
