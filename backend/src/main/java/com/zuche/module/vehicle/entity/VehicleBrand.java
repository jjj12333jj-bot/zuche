package com.zuche.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车辆品牌（对应 vehicle_brand）
 */
@Data
@TableName("vehicle_brand")
public class VehicleBrand {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String brandName;

    private String brandLogo;

    private String initial;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
