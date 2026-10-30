package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆调价
 *
 * <p>日租金与押金一次改完：这两个价格在业务上是一起看的（押金通常≈日租金×5），
 * 分两个接口改容易出现「租金改了、押金还是旧的」的中间态。</p>
 */
@Data
@Schema(description = "车辆调价")
public class VehiclePriceDTO {

    @NotNull(message = "日租金不能为空")
    @DecimalMin(value = "1.00", message = "日租金不能低于 1 元")
    @DecimalMax(value = "99999.00", message = "日租金不能高于 99999 元")
    private BigDecimal dailyPrice;

    @NotNull(message = "押金不能为空")
    @DecimalMin(value = "0.00", message = "押金不能为负数")
    @DecimalMax(value = "999999.00", message = "押金不能高于 999999 元")
    private BigDecimal deposit;

    @Schema(description = "改价原因，选填，会写进操作提示")
    private String remark;
}
