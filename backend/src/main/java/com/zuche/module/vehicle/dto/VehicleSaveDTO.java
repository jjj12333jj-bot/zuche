package com.zuche.module.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import java.time.LocalDate;

/**
 * 车辆档案新增 / 编辑
 *
 * <p>不接收 brandId：品牌由所选的车型反查得到。允许单独填品牌，
 * 必然会出现「车型是大众朗逸、品牌填了丰田」这种自相矛盾的档案。</p>
 */
@Data
@Schema(description = "车辆档案新增或编辑")
public class VehicleSaveDTO {

    @NotBlank(message = "车辆编号不能为空")
    @Size(max = 32, message = "车辆编号最长 32 个字符")
    private String vehicleNo;

    @NotNull(message = "车型不能为空")
    private Long modelId;

    @NotNull(message = "所属网点不能为空")
    private Long storeId;

    @NotBlank(message = "车牌号不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5][A-Z]·?[A-Z0-9]{4,6}$",
            message = "车牌号格式不正确，例如 闽C12345 或 闽C·12345")
    private String licensePlate;

    @Schema(description = "车架号，17 位；选填")
    @Size(max = 32, message = "车架号最长 32 个字符")
    private String vin;

    @Size(max = 20, message = "颜色最长 20 个字")
    private String color;

    @Schema(description = "当前里程（万公里）")
    @DecimalMin(value = "0.00", message = "里程不能为负数")
    @DecimalMax(value = "999.99", message = "里程不能超过 999.99 万公里")
    private BigDecimal currentMileage;

    @Schema(description = "日租金；不填时取车型指导价")
    @DecimalMin(value = "1.00", message = "日租金不能低于 1 元")
    @DecimalMax(value = "99999.00", message = "日租金不能高于 99999 元")
    private BigDecimal dailyPrice;

    @Schema(description = "标准押金；不填时取车型标准押金")
    @DecimalMin(value = "0.00", message = "押金不能为负数")
    @DecimalMax(value = "999999.00", message = "押金不能高于 999999 元")
    private BigDecimal deposit;

    @Schema(description = "租金折扣，1.00 表示无折扣")
    @DecimalMin(value = "0.50", message = "折扣不能低于 0.5")
    @DecimalMax(value = "1.00", message = "折扣不能高于 1.0")
    private BigDecimal discountRate;

    @Schema(description = "上牌日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate registerDate;

    @Schema(description = "0 下架 1 可租 3 维保中；新增时只能选这三种，2 已租出由系统维护")
    @Min(value = 0, message = "状态不合法")
    @Max(value = 3, message = "状态不合法")
    private Integer status;

    @Size(max = 500, message = "描述最长 500 个字")
    private String description;
}
