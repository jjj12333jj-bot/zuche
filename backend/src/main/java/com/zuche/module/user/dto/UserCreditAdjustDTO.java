package com.zuche.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台人工调分
 *
 * <p>与信用模块的 {@code CreditAdjustDTO} 分开定义，是为了让「用户管理」页
 * 不必依赖信用模块的入参契约；后端仍走同一个 CreditService.adjustByAdmin。</p>
 */
@Data
@Schema(description = "人工调分参数")
public class UserCreditAdjustDTO {

    @Schema(description = "分值变化，正数加分、负数扣分", example = "-10")
    @NotNull(message = "调整分值不能为空")
    @Min(value = -50, message = "单次最多扣 50 分")
    @Max(value = 50, message = "单次最多加 50 分")
    private Integer scoreChange;

    @Schema(description = "调整原因，必填——没有原因的调分等于黑箱", example = "门店投诉：取车时爽约")
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 200, message = "原因不能超过 200 字")
    private String reason;
}
