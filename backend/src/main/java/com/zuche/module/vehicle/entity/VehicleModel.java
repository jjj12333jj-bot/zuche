package com.zuche.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车型（对应 vehicle_model）：承载参数、指导日租金与标准押金
 */
@Data
@TableName("vehicle_model")
public class VehicleModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long brandId;

    private String modelName;

    /** 字典 vehicle_category：ECONOMY/COMFORT/SUV/LUXURY/EV */
    private String categoryCode;

    private Integer seats;

    /** AUTO / MANUAL */
    private String gearbox;

    /** GASOLINE / HYBRID / ELECTRIC */
    private String fuelType;

    private String displacement;

    private BigDecimal guidePrice;

    private BigDecimal deposit;

    private String coverImage;

    private String description;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
