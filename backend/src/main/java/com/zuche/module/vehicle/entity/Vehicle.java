package com.zuche.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆实体（对应 vehicle）
 *
 * <p>库存不使用独立 stock 字段：一台车一行，是否可租由 status 表达，
 * 某网点的可租车辆数 = COUNT(vehicle WHERE store_id=? AND status=1)。</p>
 */
@Data
@TableName("vehicle")
@Schema(description = "车辆")
public class Vehicle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String vehicleNo;

    private Long modelId;

    /** 品牌冗余，列表查询避免多一次 join */
    private Long brandId;

    private Long storeId;

    private String licensePlate;

    private String vin;

    private String color;

    /** 当前里程（万公里） */
    private BigDecimal currentMileage;

    private BigDecimal dailyPrice;

    /** 标准押金（未乘信用系数） */
    private BigDecimal deposit;

    /** 租金折扣，1.00 表示无折扣 */
    private BigDecimal discountRate;

    private LocalDate registerDate;

    /** 1 可租 2 已租出 3 维修中 0 下架 */
    private Integer status;

    private Integer rentCount;

    private Integer viewCount;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
