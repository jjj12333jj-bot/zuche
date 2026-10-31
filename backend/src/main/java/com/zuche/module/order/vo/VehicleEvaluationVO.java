package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 车辆详情页的评价区块：摘要 + 分页列表
 *
 * <p>摘要和列表放同一个接口，是因为两者必须同时出现——只给平均分不给评价是刷分，
 * 只给评价不给平均分用户得自己数。一次返回，前端不用并发两个请求再对齐时机。</p>
 */
@Data
@Schema(description = "车辆评价区块")
public class VehicleEvaluationVO {

    @Schema(description = "综合平均分，保留一位小数")
    private Double avgRating;

    @Schema(description = "车辆状况平均分")
    private Double avgVehicleRating;

    @Schema(description = "门店服务平均分")
    private Double avgStoreRating;

    @Schema(description = "评价总数")
    private Integer total;

    @Schema(description = "5 星条数")
    private Integer star5;

    @Schema(description = "4 星条数")
    private Integer star4;

    @Schema(description = "3 星条数")
    private Integer star3;

    @Schema(description = "2 星条数")
    private Integer star2;

    @Schema(description = "1 星条数")
    private Integer star1;

    private List<VehicleEvaluationItemVO> records;

    private Long page;

    private Long size;
}
