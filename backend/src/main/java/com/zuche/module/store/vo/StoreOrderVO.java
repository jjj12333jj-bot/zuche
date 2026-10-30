package com.zuche.module.store.vo;

import com.zuche.module.order.vo.OrderListVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网点订单列表项
 *
 * <p>在 {@link OrderListVO} 之上补三个客户字段：网点员工要能一眼看出
 * "这台车是谁租的、电话多少"，否则没法联系客户催还。
 * 用户端不需要这些字段（本来就是自己的单），所以不在父类上加。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "网点订单列表项")
public class StoreOrderVO extends OrderListVO {

    @Schema(description = "客户账号")
    private String customerUsername;

    @Schema(description = "客户姓名")
    private String customerName;

    @Schema(description = "客户手机号")
    private String customerPhone;
}
