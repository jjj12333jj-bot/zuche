package com.zuche.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用申诉单（对应 credit_appeal）
 *
 * <p>申诉是"扣分可救济"的唯一通道：用户对某条扣分记录有异议时提交申诉，
 * 管理员审核通过后由 {@code CreditService.restoreByAppeal} 补分，
 * 补分同样落 credit_log（change_type = APPEAL），分数依然可复算。</p>
 */
@Data
@TableName("credit_appeal")
@Schema(description = "信用申诉")
public class CreditAppeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appealNo;

    private Long userId;

    /** 针对哪一条扣分记录，可为空（对整单申诉） */
    private Long creditLogId;

    private Long orderId;

    private String appealReason;

    /** 证据图片 JSON 数组，本项目不做上传，字段保留 */
    private String evidence;

    /** PENDING / APPROVED / REJECTED */
    private String status;

    private Long handleUserId;

    private LocalDateTime handleTime;

    private String handleRemark;

    /** 通过时补回多少分 */
    private Integer restoreScore;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
