package com.zuche.module.vehicle.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌查询参数（platform 端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "品牌查询参数")
public class BrandQueryDTO extends PageQuery {

    @Schema(description = "品牌名 / 首字母，模糊匹配")
    private String keyword;

    @Schema(description = "1 启用 0 停用，为空查全部")
    private Integer status;
}
