package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单/试算用的车辆快照
 *
 * <p>只取交易必需字段：价格、押金、折扣、归属网点、可租状态。
 * 与列表 VO 分开，避免为了算一个押金把六张表都 join 进来。</p>
 */
@Data
@Schema(description = "下单用车辆信息")
public class VehicleRentVO {

    private Long id;

    @Schema(description = "车辆名称快照：品牌 + 车型")
    private String vehicleName;

    private String brandName;

    private String modelName;

    private String licensePlate;

    @Schema(description = "日租金（未打折）")
    private BigDecimal dailyPrice;

    @Schema(description = "标准押金（未乘信用系数）")
    private BigDecimal deposit;

    @Schema(description = "租金折扣，1.00 表示无折扣")
    private BigDecimal discountRate;

    @Schema(description = "1 可租 2 已租出 3 维修 0 下架")
    private Integer status;

    private Long storeId;

    private String storeName;

    private String coverImage;
}
