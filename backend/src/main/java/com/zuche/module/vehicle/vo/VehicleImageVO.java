package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆图片
 */
@Data
@Schema(description = "车辆图片")
public class VehicleImageVO {

    private Long id;

    private String imageUrl;

    @Schema(description = "EXTERIOR / INTERIOR / LICENSE")
    private String imageType;

    private Integer sortNo;
}
