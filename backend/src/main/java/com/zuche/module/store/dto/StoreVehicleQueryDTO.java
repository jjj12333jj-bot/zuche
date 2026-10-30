package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网点车辆查询参数
 */
@Data
@Schema(description = "网点车辆查询参数")
public class StoreVehicleQueryDTO {

    @Schema(description = "车辆状态：0 下架 1 可租 2 已租出 3 维保中，为空查全部")
    private Integer status;

    @Schema(description = "车牌 / 车辆编号 / 品牌 / 车型，模糊匹配")
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
