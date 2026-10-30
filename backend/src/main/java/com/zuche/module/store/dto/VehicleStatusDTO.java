package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 车辆状态变更
 *
 * <p>只允许手工设置为 0 下架 / 1 可租 / 3 维保中。
 * 2 已租出是系统状态——由支付成功锁车、归还完成释放，手工设置会让
 * 车辆占用与订单脱节，因此 Service 层会拒绝 status=2。</p>
 */
@Data
@Schema(description = "车辆状态变更")
public class VehicleStatusDTO {

    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "状态取值只能是 0/1/3")
    @Max(value = 3, message = "状态取值只能是 0/1/3")
    private Integer status;

    @Size(max = 200, message = "变更原因最多 200 字")
    private String reason;
}
