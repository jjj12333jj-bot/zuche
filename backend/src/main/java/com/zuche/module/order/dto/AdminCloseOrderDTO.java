package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台关闭订单
 */
@Data
@Schema(description = "平台关闭订单参数")
public class AdminCloseOrderDTO {

    @Schema(description = "关闭原因，必填——事后复盘只能靠这句话", example = "客户电话要求取消")
    @NotBlank(message = "关闭原因不能为空")
    @Size(max = 200, message = "关闭原因不能超过 200 字")
    private String reason;
}
