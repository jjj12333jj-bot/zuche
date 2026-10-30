package com.zuche.module.credit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用分变动日志（对应 credit_log）
 *
 * <p>这张表是信用分的<b>唯一事实来源</b>：{@code sys_user.credit_score} 只是它的一个物化视图，
 * 恒等于 {@code clamp(100 + Σ score_change, 0, 150)}。分数对不上时，重算这张表即可恢复。</p>
 *
 * <p>唯一索引 {@code uk_dedup(order_id, action_code, dedup_key)} 承担幂等职责：
 * 定时任务、用户连点、接口重试都只会留下一条记录。</p>
 */
@Data
@TableName("credit_log")
@Schema(description = "信用分变动日志")
public class CreditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 事件码，对应 credit_rule.rule_code */
    private String actionCode;

    /** EVENT 业务事件 / BEHAVIOR 行为模型重算 / ADMIN 人工 / APPEAL 申诉 */
    private String changeType;

    private Integer scoreBefore;

    /** 实际生效的变动值（触顶/触底时会小于规则分值） */
    private Integer scoreChange;

    private Integer scoreAfter;

    private Long orderId;

    /** 幂等键；无订单场景为空串（唯一索引里 NULL 不参与比较，所以统一用空串） */
    private String dedupKey;

    private String reason;

    private Long operatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
