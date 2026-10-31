package com.zuche.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 支付参数
 */
@Data
@Schema(description = "支付参数")
public class OrderPayDTO {

    @Schema(description = "支付渠道", defaultValue = "MOCK_WECHAT",
            allowableValues = {"MOCK_WECHAT", "MOCK_ALIPAY"})
    @Pattern(regexp = "MOCK_WECHAT|MOCK_ALIPAY", message = "不支持的支付方式")
    private String payMethod = "MOCK_WECHAT";
}
