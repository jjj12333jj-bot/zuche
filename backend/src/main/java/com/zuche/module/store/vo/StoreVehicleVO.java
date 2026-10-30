package com.zuche.module.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 网点车辆列表项
 *
 * <p>{@code activeOrderNo} 不为空表示这台车正被一张未结束订单占着，
 * 此时既不能改状态也不能调出——前端据此禁用按钮，后端再校验一次。</p>
 */
@Data
@Schema(description = "网点车辆")
public class StoreVehicleVO {

    private Long id;

    private String vehicleNo;

    private String licensePlate;

    private String brandName;

    private String modelName;

    private String color;

    private Long storeId;

    private BigDecimal dailyPrice;

    private BigDecimal deposit;

    /** 租金折扣，1.00 表示无折扣 */
    private BigDecimal discountRate;

    /** 当前里程（万公里） */
    private BigDecimal currentMileage;

    /** 0 下架 1 可租 2 已租出 3 维保中 */
    private Integer status;

    private String statusLabel;

    private Integer rentCount;

    private LocalDate registerDate;

    /* ---------- 台账摘要 ---------- */

    private Integer maintCount;

    private LocalDate lastMaintDate;

    /** 下次保养日期，已过期说明该安排进厂了 */
    private LocalDate nextMaintDate;

    /* ---------- 当前占用 ---------- */

    private String activeOrderNo;

    private String activeOrderStatus;

    private String activeOrderStatusLabel;

    /** 是否有未结束订单占用，由 Service 按 activeOrderNo 推导 */
    private Boolean occupied;
}
