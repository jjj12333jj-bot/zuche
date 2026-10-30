package com.zuche.module.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台风控名单列表项
 */
@Data
@Schema(description = "风控名单")
public class CreditBlacklistVO {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private Integer creditScore;

    private String creditLevel;

    /** WARNING / OVERRIDE / BLACKLIST */
    private String listType;

    private String listTypeLabel;

    @Schema(description = "是否限制下单（OVERRIDE / BLACKLIST）")
    private Boolean restrictOrder;

    private String reason;

    private String triggerRule;

    private LocalDateTime effectiveTime;

    @Schema(description = "为空表示长期有效")
    private LocalDateTime expireTime;

    /** ACTIVE / RELEASED */
    private String status;

    private String statusLabel;

    private LocalDateTime createTime;
}
