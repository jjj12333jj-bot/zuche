package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆列表项
 */
@Data
@Schema(description = "车辆列表项")
public class VehicleListVO {

    private Long id;

    @Schema(description = "资产编号")
    private String vehicleNo;

    private Long modelId;

    private String modelName;

    private Long brandId;

    private String brandName;

    private String categoryCode;

    private String categoryName;

    private String categoryColor;

    private Integer seats;

    private String gearbox;

    private String gearboxName;

    private String fuelType;

    private String fuelTypeName;

    private String displacement;

    @Schema(description = "日租金原价")
    private BigDecimal dailyPrice;

    @Schema(description = "标准押金（未乘信用系数）")
    private BigDecimal deposit;

    @Schema(description = "租金折扣")
    private BigDecimal discountRate;

    @Schema(description = "折后日租金 = dailyPrice × discountRate")
    private BigDecimal finalPrice;

    private String coverImage;

    private Long storeId;

    private String storeName;

    @Schema(description = "1 可租 2 已租出 3 维修中 0 下架")
    private Integer status;

    private String statusName;

    private String statusColor;

    private Integer rentCount;

    private Integer viewCount;

    private String description;
}
