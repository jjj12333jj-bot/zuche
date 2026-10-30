package com.zuche.module.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保险单新增 / 编辑
 */
@Data
@Schema(description = "保险单新增或编辑")
public class InsuranceSaveDTO {

    @NotNull(message = "车辆不能为空")
    private Long vehicleId;

    @NotBlank(message = "险种不能为空")
    @Pattern(regexp = "COMPULSORY|COMMERCIAL|PASSENGER", message = "险种只能是 COMPULSORY/COMMERCIAL/PASSENGER")
    private String insuranceType;

    @NotBlank(message = "保险公司不能为空")
    @Size(max = 50, message = "保险公司最长 50 个字")
    private String insurer;

    @NotBlank(message = "保单号不能为空")
    @Size(max = 50, message = "保单号最长 50 个字符")
    private String policyNo;

    @NotNull(message = "生效日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "到期日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull(message = "保费不能为空")
    @DecimalMin(value = "0.00", message = "保费不能为负数")
    @DecimalMax(value = "999999.00", message = "保费不能高于 999999 元")
    private BigDecimal premium;
}
