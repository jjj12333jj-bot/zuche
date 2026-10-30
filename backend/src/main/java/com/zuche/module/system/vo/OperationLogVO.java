package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志列表项
 */
@Data
@Schema(description = "操作日志列表项")
public class OperationLogVO {

    private Long id;

    private Long userId;

    @Schema(description = "操作人账号（操作当时的快照）")
    private String username;

    private String module;

    private String operation;

    @Schema(description = "请求方法：GET/POST/PUT/DELETE")
    private String method;

    private String requestUri;

    @Schema(description = "请求参数 JSON，已脱敏并截断")
    private String params;

    private String ip;

    private Long costMs;

    private Integer status;

    @Schema(description = "状态中文名：成功 / 失败")
    private String statusLabel;

    private LocalDateTime createTime;
}
