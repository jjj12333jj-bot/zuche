package com.zuche.module.order.vo;

import com.zuche.module.credit.vo.CreditChangeVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 归还结算结果
 */
@Data
@Schema(description = "归还结算结果")
public class OrderReturnVO {

    private Long orderId;

    private String orderNo;

    private String status;

    private String statusLabel;

    @Schema(description = "逾期天数，0 表示按时归还")
    private Integer overdueDays;

    @Schema(description = "押金总额")
    private BigDecimal depositAmount;

    @Schema(description = "逾期违约金（从押金中扣除）")
    private BigDecimal lateFee;

    @Schema(description = "实际退还押金 = 押金 - 违约金")
    private BigDecimal depositRefunded;

    @Schema(description = "本次产生的信用变动（可能多条：逾期按天 + 归还事件 + 行为校准）")
    private List<CreditChangeVO> creditChanges;
}
