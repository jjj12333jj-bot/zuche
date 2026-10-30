package com.zuche.module.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 等级规则批量保存项
 *
 * <p>四档等级是<b>一组</b>强耦合的配置：把"优秀"下限从 120 调到 125，
 * 就必须同时把"良好"上限从 119 调到 124，否则中间 5 分没有归属。
 * 单行保存永远会落到"有缝隙"的中间态，所以等级规则只提供整组保存。</p>
 */
@Data
@Schema(description = "等级规则批量保存参数")
public class LevelRuleUpdateDTO {

    @NotNull(message = "规则 ID 不能为空")
    private Long id;

    @Size(max = 50, message = "规则名称最长 50 字")
    private String ruleName;

    @NotNull(message = "分数下限不能为空")
    @Min(value = 0, message = "分数下限不能为负")
    @Max(value = 1000, message = "分数下限过大")
    private Integer minScore;

    @NotNull(message = "分数上限不能为空")
    @Min(value = 0, message = "分数上限不能为负")
    @Max(value = 1000, message = "分数上限过大")
    private Integer maxScore;

    @NotNull(message = "押金系数不能为空")
    @DecimalMin(value = "0.10", message = "押金系数不能低于 0.10")
    @DecimalMax(value = "5.00", message = "押金系数不能高于 5.00")
    private BigDecimal depositCoefficient;

    @Size(max = 20)
    private String color;

    @Size(max = 200, message = "规则说明最长 200 字")
    private String description;
}
