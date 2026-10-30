package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 车辆列表页筛选项集合（一次请求拿全，减少首屏请求数）
 */
@Data
@Schema(description = "车辆筛选项")
public class VehicleOptionsVO {

    private List<CategoryOptionVO> categories;

    private List<BrandOptionVO> brands;

    private List<StoreOptionVO> stores;

    private PriceRangeVO priceRange;
}
