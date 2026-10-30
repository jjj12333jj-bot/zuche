package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 车辆详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "车辆详情")
public class VehicleDetailVO extends VehicleListVO {

    private String licensePlate;

    private String vin;

    private String color;

    @Schema(description = "当前里程（万公里）")
    private BigDecimal currentMileage;

    private LocalDate registerDate;

    @Schema(description = "车型卖点描述")
    private String modelDescription;

    private String storeAddress;

    private String storePhone;

    private String businessHours;

    @Schema(description = "图集：第 1 张为主图")
    private List<VehicleImageVO> images;
}
