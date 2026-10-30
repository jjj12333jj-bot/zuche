package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台规则配置页的行数据
 *
 * <p>直接返回实体也可以，但会把 {@code deleted}、逻辑删除标记暴露给前端，
 * 且后台需要"折算后的中文类型"，所以单独定一个 VO。</p>
 */
@Data
@Schema(description = "信用规则配置项")
public class CreditRuleVO {

    private Long id;

    @Schema(description = "LEVEL 等级 / ACTION 事件 / PARAM 参数")
    private String ruleType;

    @Schema(description = "类型中文名")
    private String ruleTypeLabel;

    private String ruleCode;

    private String ruleName;

    @Schema(description = "等级区间下限")
    private Integer minScore;

    @Schema(description = "等级区间上限")
    private Integer maxScore;

    @Schema(description = "押金系数")
    private BigDecimal depositCoefficient;

    @Schema(description = "事件分值 / 参数值")
    private Integer scoreValue;

    @Schema(description = "单订单最多生效次数 / 参数封顶次数")
    private Integer maxTimes;

    private String color;

    private String description;

    private Integer sortNo;

    @Schema(description = "1 启用 0 停用")
    private Integer status;

    private LocalDateTime updateTime;
}
