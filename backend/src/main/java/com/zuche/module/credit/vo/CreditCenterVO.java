package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 信用中心头部数据
 *
 * <p>一个接口把"当前状态 + 行为画像 + 等级规则"全给前端，避免信用中心页打三次请求。</p>
 */
@Data
@Schema(description = "信用中心概览")
public class CreditCenterVO {

    private Long userId;

    @Schema(description = "当前信用分 0~150")
    private Integer creditScore;

    private String creditLevel;

    private String levelLabel;

    private String levelColor;

    private String levelDescription;

    @Schema(description = "押金系数，优秀 0.80 / 良好 0.90 / 一般 1.00 / 待观察 1.20")
    private BigDecimal depositCoefficient;

    @Schema(description = "行为分（负数或 0，0 表示行为满分）")
    private Integer behaviorScore;

    @Schema(description = "按当前行为特征应得的行为分")
    private Integer behaviorTarget;

    @Schema(description = "行为统计")
    private CreditBehaviorStats stats;

    @Schema(description = "距下一等级还差多少分，已是最高等级时为 null")
    private Integer scoreToNextLevel;

    private String nextLevelLabel;

    @Schema(description = "全部等级规则")
    private List<CreditLevelVO> levels;
}
