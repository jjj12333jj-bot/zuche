package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户提交评价
 *
 * <p>拆成两个维度而不是一个总分：车况差是车的问题，服务差是人的问题，
 * 一个总分把两者搅在一起，运营既不知道该去整备车辆还是该去培训店员。
 * 综合分由后端按两项平均算出，不接受前端传，避免出现"两项 5 分、总分 1 分"。</p>
 */
@Data
@Schema(description = "提交评价参数")
public class EvaluationCreateDTO {

    @Schema(description = "车辆状况评分 1~5", example = "5")
    @NotNull(message = "请为车辆状况打分")
    @Min(value = 1, message = "车辆状况评分为 1~5 星")
    @Max(value = 5, message = "车辆状况评分为 1~5 星")
    private Integer vehicleRating;

    @Schema(description = "门店服务评分 1~5", example = "4")
    @NotNull(message = "请为门店服务打分")
    @Min(value = 1, message = "门店服务评分为 1~5 星")
    @Max(value = 5, message = "门店服务评分为 1~5 星")
    private Integer storeRating;

    @Schema(description = "评价内容 5~500 字", example = "车况很新，取车流程很快")
    @NotBlank(message = "请填写评价内容")
    @Size(min = 5, max = 500, message = "评价内容 5~500 字")
    private String content;

    @Schema(description = "图片地址，最多 3 张，选填")
    @Size(max = 3, message = "最多上传 3 张图片")
    private List<String> images;
}
