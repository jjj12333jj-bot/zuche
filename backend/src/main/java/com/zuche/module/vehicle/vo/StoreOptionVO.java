package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网点筛选项
 */
@Data
@Schema(description = "网点筛选项")
public class StoreOptionVO {

    private Long id;

    private String name;

    private String address;
}
