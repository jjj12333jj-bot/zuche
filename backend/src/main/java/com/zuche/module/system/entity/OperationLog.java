package com.zuche.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（对应 operation_log）
 *
 * <p>只增不改不删，因此没有逻辑删除字段。{@code username} 存的是「操作当时的账号」快照，
 * 不做 JOIN 回查：账号改名甚至被删之后，审计记录仍要能说清是谁干的。</p>
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String method;

    private String requestUri;

    /** 请求参数快照，已脱敏并截断 */
    private String params;

    private String ip;

    private Long costMs;

    /** 1 成功 / 0 失败 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
