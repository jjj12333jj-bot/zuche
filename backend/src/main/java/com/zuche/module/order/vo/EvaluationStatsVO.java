package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 评价统计（全量，不受分页影响）
 *
 * <p>顶部数字卡必须走这里：评价共 12 条分两页，用「当前页」算平均分，
 * 翻页就会跳变，运营会以为数据出问题了。</p>
 */
@Data
@Schema(description = "评价统计")
public class EvaluationStatsVO {

    private Integer total;

    @Schema(description = "综合平均分，保留两位")
    private Double avgRating;

    @Schema(description = "车辆状况平均分，保留两位")
    private Double avgVehicleRating;

    @Schema(description = "门店服务平均分，保留两位")
    private Double avgStoreRating;

    @Schema(description = "5 星数")
    private Integer fiveStar;

    @Schema(description = "4 星数")
    private Integer fourStar;

    @Schema(description = "3 星及以下数")
    private Integer lowStar;

    @Schema(description = "已回复数")
    private Integer replied;

    @Schema(description = "待回复数")
    private Integer unreplied;
}
