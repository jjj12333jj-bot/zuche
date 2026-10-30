package com.zuche.module.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 网点（对应 store）
 *
 * <p>注意：不存 vehicle_count。车辆数随调车实时变化，冗余字段必然不一致，
 * 统一用 COUNT(vehicle) 统计。</p>
 */
@Data
@TableName("store")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String storeCode;

    private String storeName;

    private String address;

    private String phone;

    private String businessHours;

    /** 仅存储，不做地图渲染 */
    private BigDecimal longitude;

    private BigDecimal latitude;

    private Long managerId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
