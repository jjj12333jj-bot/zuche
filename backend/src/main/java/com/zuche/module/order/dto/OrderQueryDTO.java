package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 我的订单查询参数
 */
@Data
@Schema(description = "我的订单查询参数")
public class OrderQueryDTO {

    @Schema(description = "按状态筛选，为空查全部")
    private String status;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", defaultValue = "5")
    private Integer size = 5;

    public long getCurrent() {
        return page == null || page < 1 ? 1 : page;
    }

    public long getPageSize() {
        if (size == null || size < 1) {
            return 5;
        }
        return Math.min(size, 50);
    }
}
