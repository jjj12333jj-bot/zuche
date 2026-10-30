package com.zuche.module.credit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信用规则（对应 credit_rule）
 *
 * <p>一张表承载两类规则，用 rule_type 区分，避免建两套同构的配置表：</p>
 * <ul>
 *   <li>{@code LEVEL}：等级区间 + 押金系数，如 EXCELLENT 120~150 系数 0.80</li>
 *   <li>{@code ACTION}：行为加减分，如 ORDER_FINISH +2、OVERDUE -3</li>
 * </ul>
 */
@Data
@TableName("credit_rule")
@Schema(description = "信用规则")
public class CreditRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** LEVEL 等级规则 / ACTION 行为规则 */
    private String ruleType;

    /** 等级码或行为码 */
    private String ruleCode;

    private String ruleName;

    /** 等级区间下限（含） */
    private Integer minScore;

    /** 等级区间上限（含） */
    private Integer maxScore;

    /** 押金系数 */
    private BigDecimal depositCoefficient;

    /** 行为分值（可负） */
    private Integer scoreValue;

    /** 单订单最多生效次数 */
    private Integer maxTimes;

    private String color;

    private String description;

    private Integer sortNo;

    /** 1 启用 0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
