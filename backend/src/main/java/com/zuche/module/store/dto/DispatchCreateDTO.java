package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发起调车申请
 *
 * <p>不接收 fromStoreId：调出网点永远由服务端按「车辆当前归属」推导，
 * 前端传什么都不能决定车从哪走。少一个入参就少一处可被篡改的地方。</p>
 */
@Data
@Schema(description = "发起调车申请")
public class DispatchCreateDTO {

    @NotNull(message = "请选择要调出的车辆")
    private Long vehicleId;

    @NotNull(message = "请选择调入网点")
    private Long toStoreId;

    @NotBlank(message = "调车原因必填")
    @Size(min = 5, max = 200, message = "调车原因 5~200 字")
    private String reason;
}
