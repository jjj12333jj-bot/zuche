package com.zuche.module.analytics.vo;

import com.zuche.module.dashboard.vo.TrendPointVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营收时序预测
 *
 * <p>history 是真实值（缺口补零），forecast 是从"明天"起的预测值。
 * metrics 里的误差指标全部来自滚动回测，不是拿拟合值算出来的，
 * 否则 R² 会虚高，答辩时经不起问。</p>
 */
@Data
@Schema(description = "营收时序预测")
public class RevenueForecastVO {

    @Schema(description = "历史窗口天数")
    private Integer historyDays;

    @Schema(description = "预测天数")
    private Integer horizon;

    @Schema(description = "历史真实值")
    private List<TrendPointVO> history;

    @Schema(description = "未来预测值")
    private List<ForecastPointVO> forecast;

    /* ---------- 指标 ---------- */

    @Schema(description = "近 28 天日均营收")
    private BigDecimal recentDailyAvg;

    @Schema(description = "最近 7 天合计")
    private BigDecimal lastWeekTotal;

    @Schema(description = "未来 horizon 天预测合计")
    private BigDecimal forecastTotal;

    @Schema(description = "预测日均")
    private BigDecimal forecastDailyAvg;

    @Schema(description = "预测日均相对近 28 天日均的偏差（%），正为看涨")
    private Double vsRecentAvg;

    @Schema(description = "线性趋势斜率（元/天），正为增长")
    private BigDecimal trendSlope;

    @Schema(description = "回测窗口天数")
    private Integer backtestDays;

    @Schema(description = "日粒度平均绝对误差 MAE")
    private BigDecimal mae;

    @Schema(description = "日粒度平均绝对百分比误差 MAPE（%），小分母日期会放大，仅作参考")
    private Double mape;

    @Schema(description = "80% 置信区间在回测中的实际覆盖率（%），检验区间标定是否可信")
    private Double coverage;

    @Schema(description = "周合计加权绝对百分比误差 WAPE（%），比 MAPE 更抗小分母")
    private Double weeklyWape;

    @Schema(description = "回测决定系数 R²")
    private Double r2;

    @Schema(description = "可信度 HIGH / MEDIUM / LOW")
    private String confidence;

    @Schema(description = "可信度中文")
    private String confidenceLabel;

    @Schema(description = "模型说明，前端直接展示")
    private String method;
}
