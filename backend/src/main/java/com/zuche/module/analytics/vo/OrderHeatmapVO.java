package com.zuche.module.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单时段热力图
 *
 * <p>cells 只返回有单的格子（最多 7×24 个），零值由前端补，避免为空格子传噪音。</p>
 */
@Data
@Schema(description = "下单时段热力图")
public class OrderHeatmapVO {

    @Schema(description = "统计天数")
    private Integer days;

    @Schema(description = "窗口内下单总量")
    private Integer totalOrders;

    @Schema(description = "窗口内租金合计")
    private BigDecimal totalRent;

    @Schema(description = "有单的格子")
    private List<HeatmapCellVO> cells;

    @Schema(description = "峰值星期，1=周一")
    private Integer peakWeekday;

    @Schema(description = "峰值小时")
    private Integer peakHour;

    @Schema(description = "峰值格子的单量")
    private Integer peakCount;

    @Schema(description = "各星期合计，下标 0=周一")
    private List<Integer> weekdayTotals;

    @Schema(description = "各小时合计，下标 0=0 点")
    private List<Integer> hourTotals;

    @Schema(description = "一天中最忙的小时（按全窗口合计）")
    private Integer busiestHour;

    @Schema(description = "最忙星期（按全窗口合计），1=周一")
    private Integer busiestWeekday;
}
