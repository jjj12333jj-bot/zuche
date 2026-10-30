package com.zuche.module.analytics.controller;

import com.zuche.common.Result;
import com.zuche.module.analytics.service.AnalyticsService;
import com.zuche.module.analytics.vo.ModelPreferenceVO;
import com.zuche.module.analytics.vo.OrderHeatmapVO;
import com.zuche.module.analytics.vo.RevenueForecastVO;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能分析（仅系统管理员）
 *
 * <p>三张图各自独立接口：热力图窗口是 90 天，预测窗口是 45 天，
 * 合成一个接口要么浪费带宽，要么互相迁就窗口。</p>
 */
@Tag(name = "18-智能分析", description = "下单时段热力、车型偏好矩阵、营收时序预测")
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@RequireRole(RoleCode.SYS_ADMIN)
@SecurityRequirement(name = "Authorization")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "下单时段热力图", description = "星期 × 小时的下单分布，默认 90 天")
    @GetMapping("/heatmap")
    public Result<OrderHeatmapVO> heatmap(
            @Parameter(description = "统计天数，默认 90，最大 365")
            @RequestParam(value = "days", defaultValue = "90") Integer days) {
        return Result.success(analyticsService.heatmap(days));
    }

    @Operation(summary = "车型偏好矩阵", description = "按车型分类统计订单量、客单价、营收与单车产出")
    @GetMapping("/model-preference")
    public Result<List<ModelPreferenceVO>> modelPreference(
            @Parameter(description = "统计天数，默认 90，最大 365")
            @RequestParam(value = "days", defaultValue = "90") Integer days) {
        return Result.success(analyticsService.modelPreference(days));
    }

    @Operation(summary = "营收时序预测",
            description = "季节性朴素模型 + 滚动回测，返回历史、未来预测区间与误差指标")
    @GetMapping("/revenue-forecast")
    public Result<RevenueForecastVO> revenueForecast(
            @Parameter(description = "历史窗口天数，默认 45，最大 120")
            @RequestParam(value = "days", defaultValue = "45") Integer days,
            @Parameter(description = "预测天数，默认 7，最大 14")
            @RequestParam(value = "horizon", defaultValue = "7") Integer horizon) {
        return Result.success(analyticsService.forecast(days, horizon));
    }
}
