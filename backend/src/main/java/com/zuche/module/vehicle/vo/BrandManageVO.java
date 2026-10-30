package com.zuche.module.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 品牌管理列表项
 *
 * <p>带上 modelCount / vehicleCount：品牌页最有用的信息不是「有几个品牌」，
 * 而是「每个品牌下挂了多少车型、多少台车」——停用一个品牌之前必须知道会波及多少资产。</p>
 */
@Data
@Schema(description = "品牌管理列表项")
public class BrandManageVO {

    private Long id;

    private String brandName;

    private String brandLogo;

    private String initial;

    private Integer sortNo;

    private Integer status;

    @Schema(description = "该品牌下的车型数")
    private Integer modelCount;

    @Schema(description = "该品牌下的车辆数（含各状态）")
    private Integer vehicleCount;

    @Schema(description = "该品牌下当前可租车辆数")
    private Integer rentableCount;

    private LocalDateTime createTime;
}
