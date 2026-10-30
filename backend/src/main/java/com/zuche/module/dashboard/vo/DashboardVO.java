package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营看板聚合结果
 *
 * <p>一个接口返回全部区块，前端一次渲染，避免多请求造成的"图表逐个跳出来"。</p>
 */
@Data
@Schema(description = "运营看板数据")
public class DashboardVO {

    @Schema(description = "趋势统计天数")
    private Integer days;

    @Schema(description = "数据生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "指标卡")
    private KpiVO kpi;

    @Schema(description = "营收趋势")
    private List<TrendPointVO> trend;

    @Schema(description = "履约漏斗")
    private List<FunnelStepVO> funnel;

    @Schema(description = "车辆状态分布")
    private List<NameValueVO> vehicleStatus;

    @Schema(description = "网点经营")
    private List<StoreStatVO> stores;

    @Schema(description = "车型营收排行")
    private List<ModelStatVO> models;

    @Schema(description = "信用等级分布")
    private List<CreditLevelStatVO> creditLevels;
}
