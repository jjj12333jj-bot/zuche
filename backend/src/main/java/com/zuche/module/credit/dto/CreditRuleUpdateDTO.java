package com.zuche.module.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台修改信用规则
 *
 * <p>只允许改"值"，不允许改 {@code ruleType / ruleCode}：
 * 编码是代码里的联结点（如 OVERDUE 对应逾期事件），改了会造成历史日志找不到规则。
 * 需要新规则请走 SQL 迁移，而不是在页面上随手新建。</p>
 */
@Data
@Schema(description = "信用规则修改参数")
public class CreditRuleUpdateDTO {

    @Size(max = 50, message = "规则名称最长 50 字")
    private String ruleName;

    @Min(value = 0, message = "分数下限不能为负")
    @Max(value = 1000, message = "分数下限过大")
    private Integer minScore;

    @Min(value = 0, message = "分数上限不能为负")
    @Max(value = 1000, message = "分数上限过大")
    private Integer maxScore;

    @DecimalMin(value = "0.10", message = "押金系数不能低于 0.10")
    @DecimalMax(value = "5.00", message = "押金系数不能高于 5.00")
    private BigDecimal depositCoefficient;

    @Min(value = -100, message = "分值不能低于 -100")
    @Max(value = 1000, message = "分值不能高于 1000")
    private Integer scoreValue;

    @Min(value = 1, message = "生效次数至少为 1")
    @Max(value = 365, message = "生效次数不能超过 365")
    private Integer maxTimes;

    @Size(max = 20)
    private String color;

    @Size(max = 200, message = "规则说明最长 200 字")
    private String description;

    @Schema(description = "1 启用 0 停用；等级规则不允许停用")
    private Integer status;
}
