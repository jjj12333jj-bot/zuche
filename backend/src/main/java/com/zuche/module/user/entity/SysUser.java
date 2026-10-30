package com.zuche.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应 sys_user）
 *
 * <p>creditScore 恒等于 100 + Σ credit_log.score_change（0~150），
 * 任何改动都必须经 CreditService 写入日志，禁止直接 update 分数。</p>
 */
@Data
@TableName("sys_user")
@Schema(description = "用户")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @Schema(description = "BCrypt 密文，不返回前端")
    private String password;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    private String idCard;

    private String driverLicense;

    /** 信用分 0~150 */
    private Integer creditScore;

    /** 信用等级 EXCELLENT/GOOD/NORMAL/WATCH */
    private String creditLevel;

    /** 网点管理员所属网点 */
    private Long storeId;

    /** 1 正常 0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
