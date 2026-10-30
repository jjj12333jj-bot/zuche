package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆档案（platform 端，跨网点）
 */
@Data
@Schema(description = "车辆档案")
public class AdminVehicleVO {

    private Long id;

    private String vehicleNo;

    private String licensePlate;

    private String vin;

    private String color;

    private Long brandId;

    private String brandName;

    private Long modelId;

    private String modelName;

    private String categoryCode;

    private Long storeId;

    private String storeName;

    private BigDecimal currentMileage;

    private BigDecimal dailyPrice;

    private BigDecimal deposit;

    private BigDecimal discountRate;

    private LocalDate registerDate;

    private Integer status;

    /** 0 下架 1 可租 2 已租出 3 维保中 */
    private String statusLabel;

    private Integer rentCount;

    private Integer viewCount;

    private String description;

    @Schema(description = "当前占用的订单号；无活跃订单时为空")
    private String activeOrderNo;

    @Schema(description = "当前占用订单的状态")
    private String activeOrderStatus;

    @Schema(description = "当前占用订单的取车日期")
    private LocalDate activeStartDate;

    @Schema(description = "当前占用订单的归还日期")
    private LocalDate activeEndDate;

    @Schema(description = "最晚一张保单的到期日")
    private LocalDate insuranceEndDate;

    @Schema(description = "最近一次保养的下次保养日")
    private LocalDate nextMaintainDate;

    private LocalDateTime createTime;
}
