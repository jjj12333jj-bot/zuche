package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 信用快照：某一时刻用户的信用状态与押金系数
 *
 * <p>下单时会把 score/level/coefficient 三个值写进 lease_order 快照字段，
 * 之后用户信用分再变，历史订单的押金金额也不会漂移。</p>
 */
@Data
@Schema(description = "信用快照")
public class CreditSnapshot {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "信用分 0~150")
    private Integer creditScore;

    @Schema(description = "信用等级码 EXCELLENT/GOOD/NORMAL/WATCH")
    private String creditLevel;

    @Schema(description = "信用等级中文名")
    private String levelLabel;

    @Schema(description = "押金系数，优秀 0.80 / 良好 0.90 / 一般 1.00 / 待观察 1.20")
    private BigDecimal depositCoefficient;

    @Schema(description = "等级标签颜色")
    private String levelColor;

    @Schema(description = "等级说明，用于前端展示")
    private String levelDescription;
}
