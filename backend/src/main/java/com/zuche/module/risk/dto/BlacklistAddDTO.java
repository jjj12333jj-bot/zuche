package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手工加入风控名单
 */
@Data
@Schema(description = "加入风控名单")
public class BlacklistAddDTO {

    @NotNull(message = "请选择用户")
    private Long userId;

    @Schema(description = "WARNING 预警 / OVERRIDE 限制 / BLACKLIST 黑名单")
    private String listType;

    @NotBlank(message = "加入原因必填")
    @Size(max = 200, message = "原因最长 200 字")
    private String reason;

    @Schema(description = "有效天数，不传表示长期")
    @Min(value = 1, message = "有效期至少 1 天")
    @Max(value = 3650, message = "有效期不能超过 3650 天")
    private Integer expireDays;
}
