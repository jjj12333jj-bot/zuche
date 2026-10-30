package com.zuche.module.vehicle.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保养维修台账查询参数（platform 端，跨网点）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "保养维修台账查询参数")
public class AdminMaintenanceQueryDTO extends PageQuery {

    @Schema(description = "台账类型：MAINTAIN 保养 / REPAIR 维修")
    private String maintType;

    @Schema(description = "网点 ID")
    private Long storeId;

    @Schema(description = "限定某台车")
    private Long vehicleId;

    @Schema(description = "车牌 / 车辆编号 / 作业内容，模糊匹配")
    private String keyword;
}
