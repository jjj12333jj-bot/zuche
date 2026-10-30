package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 租赁试算参数（下单页实时算钱，不落库）
 */
@Data
@Schema(description = "租赁试算参数")
public class OrderQuoteDTO {

    @Schema(description = "车辆ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择车辆")
    private Long vehicleId;

    @Schema(description = "租期开始日期 yyyy-MM-dd", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择取车日期")
    private LocalDate startDate;

    @Schema(description = "租期结束日期 yyyy-MM-dd", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择还车日期")
    private LocalDate endDate;

    @Schema(description = "租车数量；S2 为单车下单，固定为 1（字段为按车型批量下单预留）", defaultValue = "1")
    @Min(value = 1, message = "租车数量至少 1 台")
    @Max(value = 1, message = "当前为单车租赁，数量必须为 1")
    private Integer quantity = 1;
}
