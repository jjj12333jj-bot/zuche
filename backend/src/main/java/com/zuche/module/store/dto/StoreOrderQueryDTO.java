package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网点订单查询参数
 *
 * <p>与用户端 {@code OrderQueryDTO} 分开：这边多了 keyword（订单号/客户），
 * 默认每页也更大（网点要一屏看到更多待办），
 * 而且两个端的筛选语义不同，混用容易在后续加字段时互相干扰。</p>
 */
@Data
@Schema(description = "网点订单查询参数")
public class StoreOrderQueryDTO {

    @Schema(description = "按状态筛选，为空查全部")
    private String status;

    @Schema(description = "订单号 / 用户名 / 真实姓名 / 手机号，模糊匹配")
    private String keyword;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 50", defaultValue = "10")
    private Integer size = 10;

    public long getCurrent() {
        return page == null || page < 1 ? 1 : page;
    }

    public long getPageSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }
}
