package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增保养/维修台账
 */
@Data
@Schema(description = "新增车辆台账")
public class MaintenanceCreateDTO {

    @NotBlank(message = "台账类型必填")
    @Pattern(regexp = "MAINTAIN|REPAIR", message = "台账类型只能是 MAINTAIN（保养）或 REPAIR（维修）")
    private String maintType;

    @NotNull(message = "进场日期不能为空")
    private LocalDate maintDate;

    @DecimalMin(value = "0.0", message = "里程不能为负数")
    private BigDecimal mileage;

    @DecimalMin(value = "0.00", message = "费用不能为负数")
    private BigDecimal cost;

    @NotBlank(message = "作业内容必填")
    @Size(min = 4, max = 500, message = "作业内容 4~500 字")
    private String content;

    @Schema(description = "下次保养日期，选填")
    private LocalDate nextDate;

    @Size(max = 200, message = "备注最多 200 字")
    private String remark;
}
