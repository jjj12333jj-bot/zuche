package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单参数
 */
@Data
@Schema(description = "取消订单参数")
public class OrderCancelDTO {

    @Schema(description = "取消原因", defaultValue = "用户主动取消")
    @Size(max = 200, message = "取消原因最多 200 字")
    private String reason;
}
