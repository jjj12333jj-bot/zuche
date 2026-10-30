package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 可租车辆价格区间（按折后价统计）
 */
@Data
@Schema(description = "价格区间")
public class PriceRangeVO {

    private BigDecimal minPrice;

    private BigDecimal maxPrice;
}
