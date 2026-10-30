package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台订单详情
 *
 * <p>字段与用户端 {@link OrderDetailVO} 完全一致，只多三个客户字段。
 * 不直接把用户端 VO 暴露给平台端，是因为两者的「可见范围」不同：
 * 将来用户端要加脱敏字段时，不会顺手把平台端也一起脱了。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台订单详情")
public class AdminOrderDetailVO extends OrderDetailVO {

    private String customerUsername;

    private String customerName;

    private String customerPhone;

    @Schema(description = "客户当前信用分（实时，不是下单快照）")
    private Integer customerCreditScore;

    @Schema(description = "客户当前信用等级")
    private String customerCreditLevel;
}
