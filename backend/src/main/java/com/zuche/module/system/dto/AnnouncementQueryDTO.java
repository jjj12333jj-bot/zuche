package com.zuche.module.system.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台公告查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台公告查询参数")
public class AnnouncementQueryDTO extends PageQuery {

    @Schema(description = "标题 / 正文，模糊匹配")
    private String keyword;

    @Schema(description = "类型：NOTICE 公告 / ACTIVITY 活动 / RULE 规则，为空查全部")
    private String type;

    @Schema(description = "状态：1 已发布 / 0 草稿，为空查全部")
    private Integer status;
}
