package com.zuche.module.credit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用明细列表项（信用中心页面直接渲染）
 */
@Data
@Schema(description = "信用明细")
public class CreditLogVO {

    private Long id;

    @Schema(description = "事件码")
    private String actionCode;

    @Schema(description = "事件名称，取自 credit_rule.rule_name")
    private String actionName;

    @Schema(description = "EVENT/BEHAVIOR/ADMIN/APPEAL")
    private String changeType;

    @Schema(description = "变动类型中文名")
    private String changeTypeLabel;

    private Integer scoreBefore;

    private Integer scoreChange;

    private Integer scoreAfter;

    private Long orderId;

    @Schema(description = "关联订单号")
    private String orderNo;

    private String reason;

    private LocalDateTime createTime;
}
