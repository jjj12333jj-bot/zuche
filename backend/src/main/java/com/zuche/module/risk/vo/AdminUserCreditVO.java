package com.zuche.module.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台「用户信用」列表项
 */
@Data
@Schema(description = "后台用户信用信息")
public class AdminUserCreditVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private Integer creditScore;

    private String creditLevel;

    private String levelLabel;

    private String levelColor;

    @Schema(description = "当前生效的风控类型 WARNING/OVERRIDE/BLACKLIST，无则为空")
    private String riskType;

    private String riskTypeLabel;

    @Schema(description = "累计已完成订单数")
    private Integer finishedCount;

    @Schema(description = "累计支付后取消订单数")
    private Integer cancelCount;

    private LocalDateTime createTime;
}
