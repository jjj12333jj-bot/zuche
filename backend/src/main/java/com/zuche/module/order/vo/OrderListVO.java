package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单列表项
 */
@Data
@Schema(description = "订单列表项")
public class OrderListVO {

    private Long id;

    private String orderNo;

    private String status;

    @Schema(description = "状态中文名（服务层填充）")
    private String statusLabel;

    private Long storeId;

    private String storeName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer rentDays;

    private Integer vehicleCount;

    private BigDecimal rentAmount;

    private BigDecimal depositAmount;

    private BigDecimal totalAmount;

    @Schema(description = "下单时信用等级快照")
    private String creditLevelSnapshot;

    private String creditLevelLabel;

    @Schema(description = "下单时押金系数快照")
    private BigDecimal depositCoefficient;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    @Schema(description = "车辆名称，多台用 / 分隔")
    private String vehicleNames;

    private String coverImage;

    @Schema(description = "首台车ID，用于再次下单跳转")
    private Long firstVehicleId;

    @Schema(description = "是否可支付（仅待支付）")
    private Boolean canPay;

    @Schema(description = "是否可取消（待支付 / 待取车）")
    private Boolean canCancel;

    @Schema(description = "是否可确认取车（仅待取车）")
    private Boolean canPickup;

    @Schema(description = "是否可申请归还（已取车 / 逾期）")
    private Boolean canApplyReturn;

    @Schema(description = "是否可确认归还（仅待归还）")
    private Boolean canConfirmReturn;

    @Schema(description = "是否可评价（已完成且尚未评价）")
    private Boolean canEvaluate;

    @Schema(description = "是否已评价")
    private Boolean evaluated;
}
