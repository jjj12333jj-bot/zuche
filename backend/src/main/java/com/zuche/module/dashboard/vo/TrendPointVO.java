package com.zuche.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 营收趋势的一天
 */
@Data
@Schema(description = "营收趋势点")
public class TrendPointVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "当日租金实收")
    private BigDecimal rentAmount;

    @Schema(description = "当日支付订单数")
    private Integer orderCount;

    @Schema(description = "当日违约金")
    private BigDecimal lateFee;
}
