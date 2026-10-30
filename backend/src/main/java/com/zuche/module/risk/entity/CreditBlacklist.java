package com.zuche.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控名单（对应 credit_blacklist）
 *
 * <p>三档强度，含义完全不同，不能混用：</p>
 * <ul>
 *   <li>{@code WARNING} 预警：只记录，不限制下单，用于人工关注</li>
 *   <li>{@code OVERRIDE} 限制：下单前需要人工放行（本项目简化为禁止下单）</li>
 *   <li>{@code BLACKLIST} 黑名单：直接拒绝下单</li>
 * </ul>
 */
@Data
@TableName("credit_blacklist")
@Schema(description = "风控名单")
public class CreditBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** WARNING / OVERRIDE / BLACKLIST */
    private String listType;

    private String reason;

    /** 触发规则描述，如 "逾期 8 天"、"信用分 52 低于 60" */
    private String triggerRule;

    private LocalDateTime effectiveTime;

    /** 为空表示长期有效 */
    private LocalDateTime expireTime;

    /** ACTIVE / RELEASED */
    private String status;

    /** 系统自动加入时为空 */
    private Long operatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
