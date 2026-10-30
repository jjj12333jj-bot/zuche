package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保养维修台账（platform 端，带车辆与网点信息）
 */
@Data
@Schema(description = "保养维修台账")
public class AdminMaintenanceVO {

    private Long id;

    private Long vehicleId;

    private String vehicleNo;

    private String licensePlate;

    private String modelName;

    private String brandName;

    private Long storeId;

    private String storeName;

    private String maintType;

    private String maintLabel;

    private LocalDate maintDate;

    private BigDecimal mileage;

    private BigDecimal cost;

    private String content;

    private String operatorName;

    private LocalDate nextDate;

    private String remark;

    private LocalDateTime createTime;
}
