package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车型分类筛选项（来自 sys_dict_data.vehicle_category）
 */
@Data
@Schema(description = "车型分类筛选项")
public class CategoryOptionVO {

    private String code;

    private String label;

    @Schema(description = "前端标签颜色")
    private String color;
}
