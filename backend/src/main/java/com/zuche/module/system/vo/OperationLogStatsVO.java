package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 操作日志统计卡
 */
@Data
@Schema(description = "操作日志统计")
public class OperationLogStatsVO {

    @Schema(description = "日志总条数")
    private Long total;

    @Schema(description = "今日操作数")
    private Long todayCount;

    @Schema(description = "失败操作数")
    private Long failCount;

    @Schema(description = "平均耗时（毫秒）")
    private Long avgCost;
}
