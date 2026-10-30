package com.zuche.module.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车型新增 / 编辑
 */
@Data
@Schema(description = "车型新增或编辑")
public class ModelSaveDTO {

    @NotNull(message = "所属品牌不能为空")
    private Long brandId;

    @NotBlank(message = "车型名不能为空")
    @Size(max = 50, message = "车型名最长 50 个字")
    private String modelName;

    @NotBlank(message = "车型分类不能为空")
    @Pattern(regexp = "ECONOMY|COMFORT|SUV|LUXURY|EV", message = "车型分类只能是 ECONOMY/COMFORT/SUV/LUXURY/EV")
    private String categoryCode;

    @NotNull(message = "座位数不能为空")
    @Min(value = 2, message = "座位数不能少于 2")
    @Max(value = 9, message = "座位数不能超过 9")
    private Integer seats;

    @NotBlank(message = "变速箱不能为空")
    @Pattern(regexp = "AUTO|MANUAL", message = "变速箱只能是 AUTO 或 MANUAL")
    private String gearbox;

    @NotBlank(message = "燃料类型不能为空")
    @Pattern(regexp = "GASOLINE|HYBRID|ELECTRIC", message = "燃料类型只能是 GASOLINE/HYBRID/ELECTRIC")
    private String fuelType;

    @Schema(description = "排量，纯电可填「纯电」")
    @Size(max = 20, message = "排量描述过长")
    private String displacement;

    @NotNull(message = "指导日租金不能为空")
    @DecimalMin(value = "1.00", message = "指导日租金不能低于 1 元")
    @DecimalMax(value = "99999.00", message = "指导日租金不能高于 99999 元")
    private BigDecimal guidePrice;

    @NotNull(message = "标准押金不能为空")
    @DecimalMin(value = "0.00", message = "标准押金不能为负数")
    @DecimalMax(value = "999999.00", message = "标准押金不能高于 999999 元")
    private BigDecimal deposit;

    @Schema(description = "封面图地址")
    @Size(max = 255, message = "封面图地址过长")
    private String coverImage;

    @Size(max = 500, message = "描述最长 500 个字")
    private String description;

    @Min(value = 0, message = "排序号不能为负")
    @Max(value = 9999, message = "排序号不能超过 9999")
    private Integer sortNo;

    @Schema(description = "1 启用 0 停用")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
