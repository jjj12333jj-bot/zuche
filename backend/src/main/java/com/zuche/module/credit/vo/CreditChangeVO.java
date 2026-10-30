package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 一次信用变动的结果
 *
 * <p>下单取消、归还结算等接口会把它带回前端，用于"信用分 -3"这种即时提示，
 * 免去前端再发一次请求才知道分数变了。</p>
 */
@Data
@Schema(description = "信用变动结果")
public class CreditChangeVO {

    private String actionCode;

    private String actionName;

    @Schema(description = "变动前分数")
    private Integer scoreBefore;

    @Schema(description = "实际变动值")
    private Integer scoreChange;

    @Schema(description = "变动后分数")
    private Integer scoreAfter;

    private String levelBefore;

    private String levelAfter;

    private String levelAfterLabel;

    @Schema(description = "等级是否发生变化")
    private Boolean levelChanged;

    @Schema(description = "是否因重复事件被幂等拦下")
    private Boolean duplicated;
}
