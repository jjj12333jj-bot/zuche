package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员处理申诉
 */
@Data
@Schema(description = "申诉处理参数")
public class CreditAppealHandleDTO {

    @NotNull(message = "请选择通过或驳回")
    private Boolean approved;

    @Schema(description = "通过时补回的分值，不传则按原扣分记录的绝对值补回")
    @Min(value = 1, message = "补回分值至少 1 分")
    @Max(value = 50, message = "单次最多补回 50 分")
    private Integer restoreScore;

    @Size(max = 200, message = "处理意见最长 200 字")
    private String remark;
}
