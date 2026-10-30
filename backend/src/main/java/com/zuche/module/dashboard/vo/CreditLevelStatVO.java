package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 信用等级分布
 *
 * <p>这一组数据是"信用风控"与"运营"的交叉点：
 * 高等级用户是否真的更少逾期、贡献更多订单，用数字回答。</p>
 */
@Data
@Schema(description = "信用等级分布")
public class CreditLevelStatVO {

    @Schema(description = "等级编码")
    private String level;

    @Schema(description = "等级名称")
    private String levelLabel;

    @Schema(description = "等级颜色")
    private String color;

    @Schema(description = "人数")
    private Integer userCount;

    @Schema(description = "人数占比（%）")
    private Double userRatio;

    @Schema(description = "平均信用分")
    private BigDecimal avgScore;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "逾期订单数")
    private Integer overdueCount;

    @Schema(description = "逾期率（%）")
    private Double overdueRate;

    @Schema(description = "租金贡献")
    private BigDecimal rentAmount;
}
