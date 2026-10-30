package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台公告列表项
 */
@Data
@Schema(description = "平台公告列表项")
public class AnnouncementVO {

    private Long id;

    private String title;

    private String content;

    @Schema(description = "NOTICE / ACTIVITY / RULE")
    private String type;

    @Schema(description = "类型中文名")
    private String typeLabel;

    private Long publisherId;

    @Schema(description = "发布人姓名（子查询得到，发布人被删则显示账号或空）")
    private String publisherName;

    private Integer status;

    @Schema(description = "状态中文名：已发布 / 草稿")
    private String statusLabel;

    private Integer viewCount;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;
}
