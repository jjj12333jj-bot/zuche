package com.zuche.module.store.entity;

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
 * 车辆保养/维修台账（对应 vehicle_maintenance）
 *
 * <p>只增不改：台账是资产履历，写错了也应该补一条更正记录，
 * 而不是抹掉历史。所以本模块没有编辑与删除接口。</p>
 */
@Data
@TableName("vehicle_maintenance")
@Schema(description = "车辆保养维修台账")
public class VehicleMaintenance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long vehicleId;

    /** MAINTAIN 保养 / REPAIR 维修 */
    private String maintType;

    private LocalDate maintDate;

    /** 进场里程（万公里） */
    private BigDecimal mileage;

    private BigDecimal cost;

    private String content;

    /** 登记人（网点管理员） */
    private Long operatorId;

    /** 下次保养日期，为空表示无需提醒 */
    private LocalDate nextDate;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
