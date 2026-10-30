package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车型管理列表项
 */
@Data
@Schema(description = "车型管理列表项")
public class ModelManageVO {

    private Long id;

    private Long brandId;

    private String brandName;

    private String modelName;

    private String categoryCode;

    @Schema(description = "分类中文名，由服务层翻译，前端不再维护一份字典")
    private String categoryLabel;

    private Integer seats;

    private String gearbox;

    private String gearboxLabel;

    private String fuelType;

    private String fuelLabel;

    private String displacement;

    private BigDecimal guidePrice;

    private BigDecimal deposit;

    private String coverImage;

    private String description;

    private Integer sortNo;

    private Integer status;

    @Schema(description = "该车型下的车辆数")
    private Integer vehicleCount;

    @Schema(description = "该车型下的实际最低日租金，用于发现「车型指导价与在库车价脱节」")
    private BigDecimal minDailyPrice;

    private LocalDateTime createTime;
}
