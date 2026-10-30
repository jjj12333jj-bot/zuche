package com.zuche.module.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 信用事件（写入 credit_log 的入参）
 *
 * <p>dedupKey 是幂等键：与 order_id + action_code 组成唯一索引，
 * 定时任务重复扫描同一笔逾期订单时，也不会重复扣分。</p>
 */
@Data
@Schema(description = "信用事件")
public class CreditEvent {

    @Schema(description = "关联订单ID，人工调整时可为空")
    private Long orderId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "行为码：ORDER_FINISH/ON_TIME_RETURN/OVERDUE/OVERDUE_RETURN/CANCEL_PAID/BEHAVIOR_ADJUST")
    private String actionCode;

    @Schema(description = "幂等键，如同一订单同一天传 overdue-day-1")
    private String dedupKey;

    @Schema(description = "覆盖分值；为空时取 credit_rule.score_value")
    private Integer scoreChange;

    @Schema(description = "备注，会展示在信用明细里")
    private String remark;

    @Schema(description = "操作人（人工调整时填写）")
    private Long operatorId;
}
