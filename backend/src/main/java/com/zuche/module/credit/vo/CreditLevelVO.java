package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 信用等级说明项（来自 credit_rule 的 LEVEL 规则）
 *
 * <p>前端的"等级说明"表格直接用这份数据渲染，不在前端写死 120/0.8 之类的阈值，
 * 后台改阈值后页面自动跟着变。</p>
 */
@Data
@Schema(description = "信用等级规则")
public class CreditLevelVO {

    private String code;

    private String name;

    private Integer minScore;

    private Integer maxScore;

    private BigDecimal depositCoefficient;

    private String color;

    private String description;

    @Schema(description = "是否当前用户所处等级")
    private Boolean current;
}
