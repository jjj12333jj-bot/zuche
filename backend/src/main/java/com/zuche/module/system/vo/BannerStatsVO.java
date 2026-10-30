package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 轮播图统计卡
 */
@Data
@Schema(description = "轮播图统计")
public class BannerStatsVO {

    @Schema(description = "轮播图总数")
    private Integer total;

    @Schema(description = "显示中")
    private Integer showing;

    @Schema(description = "已隐藏")
    private Integer hidden;
}
