package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 网点/平台回复评价
 */
@Data
@Schema(description = "回复评价参数")
public class EvaluationReplyDTO {

    @Schema(description = "回复内容", example = "感谢反馈，内饰问题已安排整备")
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容不能超过 500 字")
    private String reply;
}
