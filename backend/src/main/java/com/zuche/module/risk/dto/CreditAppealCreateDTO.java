package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交信用申诉
 */
@Data
@Schema(description = "提交信用申诉")
public class CreditAppealCreateDTO {

    @NotNull(message = "请选择要申诉的扣分记录")
    private Long creditLogId;

    @NotBlank(message = "申诉理由必填")
    @Size(min = 10, max = 500, message = "申诉理由 10~500 字")
    private String reason;
}
