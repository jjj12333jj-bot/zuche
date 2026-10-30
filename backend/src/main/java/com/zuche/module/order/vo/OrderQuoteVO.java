package com.zuche.module.order.vo;

import com.zuche.module.credit.vo.CreditSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 租赁试算结果
 *
 * <p>把"押金为什么是这个数"完整摊开给用户看：
 * 标准押金 × 系数 = 应付押金，同时给出减免金额，这是信用风控最直观的体感。</p>
 */
@Data
@Schema(description = "租赁试算结果")
public class OrderQuoteVO {

    private Long vehicleId;

    private String vehicleName;

    private String brandName;

    private String modelName;

    private String licensePlate;

    private String coverImage;

    private Long storeId;

    private String storeName;

    private LocalDate startDate;

    private LocalDate endDate;

    @Schema(description = "租期天数")
    private Integer rentDays;

    private Integer quantity;

    @Schema(description = "日租金（原价）")
    private BigDecimal dailyPrice;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "折后日租金")
    private BigDecimal finalDailyPrice;

    @Schema(description = "租金合计 = 折后日租金 × 天数 × 数量")
    private BigDecimal rentAmount;

    @Schema(description = "标准押金（单台，未乘系数）")
    private BigDecimal depositStandard;

    @Schema(description = "信用等级押金系数")
    private BigDecimal depositCoefficient;

    @Schema(description = "应付押金 = 标准押金 × 系数 × 数量")
    private BigDecimal depositAmount;

    @Schema(description = "信用减免金额 = 标准押金×数量 − 应付押金")
    private BigDecimal depositSaved;

    @Schema(description = "应付合计 = 租金 + 押金")
    private BigDecimal totalAmount;

    @Schema(description = "下单人信用快照")
    private CreditSnapshot credit;
}
