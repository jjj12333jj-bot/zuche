package com.zuche.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通用状态变更入参
 *
 * <p>只做「非空」校验，取值范围由各服务自己判断——
 * 品牌/车型是 0/1，车辆是 0/1/3，用注解写死反而要复制三份。</p>
 */
@Data
@Schema(description = "状态变更")
public class StatusDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer status;

    @Schema(description = "变更原因，选填，会回显在提示里")
    @Size(max = 200, message = "原因最多 200 字")
    private String reason;
}
