package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 品牌筛选项
 */
@Data
@Schema(description = "品牌筛选项")
public class BrandOptionVO {

    private Long id;

    private String name;

    private String logo;

    private String initial;
}
