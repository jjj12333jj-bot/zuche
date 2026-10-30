package com.zuche.module.vehicle.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 车型查询参数（platform 端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "车型查询参数")
public class ModelQueryDTO extends PageQuery {

    @Schema(description = "品牌 ID，为空查全部品牌")
    private Long brandId;

    @Schema(description = "车型名，模糊匹配")
    private String keyword;

    @Schema(description = "车型分类：ECONOMY/COMFORT/SUV/LUXURY/EV")
    private String categoryCode;

    @Schema(description = "1 启用 0 停用，为空查全部")
    private Integer status;
}
