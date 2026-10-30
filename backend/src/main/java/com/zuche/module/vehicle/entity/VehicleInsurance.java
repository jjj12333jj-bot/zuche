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
 * 车辆保险单（对应 vehicle_insurance）
 *
 * <p>一台车同一险种在同一时段只应有一张有效保单。这里不做唯一索引，
 * 而是由服务层在新增时校验「时段重叠」，因为现实中会存在老保单未到期先续保的情况，
 * 允许首尾相接但不允许交叉。</p>
 */
@Data
@TableName("vehicle_insurance")
@Schema(description = "车辆保险单")
public class VehicleInsurance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long vehicleId;

    /** COMPULSORY 交强险 / COMMERCIAL 商业险 / PASSENGER 座位险 */
    private String insuranceType;

    /** 保险公司 */
    private String insurer;

    /** 保单号 */
    private String policyNo;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 保费 */
    private BigDecimal premium;

    /** 1 生效中 0 已失效 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
