package com.zuche.module.vehicle.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 车辆档案查询参数（platform 端，跨网点）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "车辆档案查询参数")
public class AdminVehicleQueryDTO extends PageQuery {

    @Schema(description = "车辆状态：0 下架 1 可租 2 已租出 3 维保中，为空查全部")
    private Integer status;

    @Schema(description = "网点 ID，为空查全部网点")
    private Long storeId;

    @Schema(description = "品牌 ID")
    private Long brandId;

    @Schema(description = "车牌 / 车辆编号 / VIN，模糊匹配")
    private String keyword;
}
