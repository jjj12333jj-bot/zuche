package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台订单列表项
 *
 * <p>在 {@link OrderListVO} 上补三类平台侧字段：</p>
 * <ul>
 *   <li>客户信息——客服要能直接打电话，不能只看到 user_id</li>
 *   <li>归还网点——异地还车的单在列表里就要能看出来</li>
 *   <li>逾期天数与违约金——平台最关心「哪些单在流血」</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台订单列表项")
public class AdminOrderVO extends OrderListVO {

    private String customerUsername;

    private String customerName;

    private String customerPhone;

    private Long returnStoreId;

    private String returnStoreName;

    @Schema(description = "逾期天数，未逾期为 0")
    private Integer overdueDays;
}
