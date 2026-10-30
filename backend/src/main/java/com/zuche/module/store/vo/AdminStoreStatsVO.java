package com.zuche.module.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台网点统计
 */
@Data
@Schema(description = "平台网点统计")
public class AdminStoreStatsVO {

    private Integer total;

    private Integer normal;

    @Schema(description = "已停用")
    private Integer disabled;

    @Schema(description = "全网点在册车辆总数")
    private Integer vehicleTotal;

    @Schema(description = "网点本月营收合计")
    private java.math.BigDecimal monthRevenue;
}
