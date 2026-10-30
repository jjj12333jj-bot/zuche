package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员人工调分
 *
 * <p>原因必填且至少 5 个字：调分是"有解释权的操作"，
 * 只写"调整"两个字的记录在审计时等于没有。</p>
 */
@Data
@Schema(description = "人工调整信用分")
public class CreditAdjustDTO {

    @NotNull(message = "调整分值不能为空")
    @Min(value = -50, message = "单次最多扣 50 分")
    @Max(value = 50, message = "单次最多加 50 分")
    private Integer scoreChange;

    @NotBlank(message = "调整原因必填")
    @Size(min = 5, max = 200, message = "调整原因 5~200 字")
    private String reason;
}
