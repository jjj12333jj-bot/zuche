package com.zuche.module.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水展示对象
 */
@Data
@Schema(description = "支付流水")
public class PaymentVO {

    private Long id;

    private String payNo;

    @Schema(description = "RENT/DEPOSIT/LATE_FEE")
    private String payType;

    @Schema(description = "类型中文名")
    private String payTypeLabel;

    private BigDecimal amount;

    @Schema(description = "MOCK_WECHAT/MOCK_ALIPAY")
    private String payMethod;

    private String status;

    private LocalDateTime payTime;

    private String remark;
}
