package com.zuche.module.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆台账列表项
 */
@Data
@Schema(description = "车辆台账")
public class MaintenanceVO {

    private Long id;

    private Long vehicleId;

    private String vehicleNo;

    private String licensePlate;

    private String maintType;

    private String maintTypeLabel;

    private LocalDate maintDate;

    private BigDecimal mileage;

    private BigDecimal cost;

    private String content;

    private Long operatorId;

    private String operatorName;

    private LocalDate nextDate;

    private String remark;

    private LocalDateTime createTime;
}
