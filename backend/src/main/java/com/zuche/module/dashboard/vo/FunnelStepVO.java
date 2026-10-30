package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 履约漏斗的一级
 *
 * <p>五级定义：下单 → 支付 → 取车 → 归还 → 完成。
 * 每一级都取自订单表自己的时间戳字段，不靠状态字段推断，
 * 这样即使订单后来被取消，历史动作也不会丢。</p>
 */
@Data
@Schema(description = "履约漏斗层级")
public class FunnelStepVO {

    @Schema(description = "层级名称")
    private String stage;

    @Schema(description = "该层订单数")
    private Integer count;

    @Schema(description = "相对上一级的转化率（%）")
    private Double rate;

    @Schema(description = "相对上一级的流失率（%）")
    private Double dropRate;

    @Schema(description = "相对首级的整体转化率（%）")
    private Double overallRate;
}
