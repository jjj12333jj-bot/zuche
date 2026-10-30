package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网点台账查询参数
 */
@Data
@Schema(description = "网点台账查询参数")
public class MaintenanceQueryDTO {

    @Schema(description = "台账类型：MAINTAIN 保养 / REPAIR 维修，为空查全部")
    private String maintType;

    @Schema(description = "限定某台车，车辆详情抽屉用")
    private Long vehicleId;

    @Schema(description = "车牌 / 车辆编号 / 作业内容，模糊匹配")
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
