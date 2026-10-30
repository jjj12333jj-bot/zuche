package com.zuche.module.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预测曲线上的一个点
 */
@Data
@Schema(description = "预测点")
public class ForecastPointVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "星期，1=周一")
    private Integer weekday;

    @Schema(description = "预测值")
    private BigDecimal value;

    @Schema(description = "区间下限（80%）")
    private BigDecimal lower;

    @Schema(description = "区间上限（80%）")
    private BigDecimal upper;

    @Schema(description = "该日预测用到的同星期历史均值（解释用）")
    private BigDecimal weekdayBase;
}
