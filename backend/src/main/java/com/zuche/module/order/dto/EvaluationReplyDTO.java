package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 网点 / 平台回复评价
 *
 * <p>只有回复权，没有删除权：让商家删自家差评，等于既当运动员又当裁判。</p>
 */
@Data
@Schema(description = "回复评价参数")
public class EvaluationReplyDTO {

    @Schema(description = "回复内容", example = "感谢反馈，内饰问题已安排整备")
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 200, message = "回复内容不能超过 200 字")
    private String reply;
}
