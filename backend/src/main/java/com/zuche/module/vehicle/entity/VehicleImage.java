package com.zuche.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车辆图片（对应 vehicle_image），sortNo=1 为主图
 */
@Data
@TableName("vehicle_image")
public class VehicleImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long vehicleId;

    private String imageUrl;

    /** EXTERIOR / INTERIOR / LICENSE */
    private String imageType;

    private Integer sortNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
