package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 创建订单参数
 *
 * <p>金额一律由后端按数据库价格重新计算，前端传的金额不参与计算，
 * 避免被改成 1 元租车。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "创建订单参数")
public class OrderCreateDTO extends OrderQuoteDTO {

    @Schema(description = "归还网点ID，为空表示在取车网点归还")
    private Long returnStoreId;

    @Schema(description = "用户备注")
    @Size(max = 200, message = "备注最多 200 字")
    private String remark;
}
