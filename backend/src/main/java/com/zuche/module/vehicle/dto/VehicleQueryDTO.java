package com.zuche.module.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆列表查询参数
 */
@Data
@Schema(description = "车辆列表查询参数")
public class VehicleQueryDTO {

    @Schema(description = "关键词：车型/品牌/车牌/资产编号")
    private String keyword;

    @Schema(description = "车型分类 ECONOMY/COMFORT/SUV/LUXURY/EV")
    private String categoryCode;

    private Long brandId;

    private Long storeId;

    @Schema(description = "最低日租金（按折后价过滤）")
    private BigDecimal minPrice;

    @Schema(description = "最高日租金（按折后价过滤）")
    private BigDecimal maxPrice;

    @Schema(description = "是否只看可租车辆", defaultValue = "false")
    private Boolean onlyAvailable = false;

    @Schema(description = "排序：default/priceAsc/priceDesc/newest/popular", defaultValue = "default")
    private String sortBy = "default";

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 100", defaultValue = "12")
    private Integer size = 12;

    public long getCurrent() {
        return page == null || page < 1 ? 1 : page;
    }

    public long getPageSize() {
        if (size == null || size < 1) {
            return 12;
        }
        return Math.min(size, 100);
    }
}
