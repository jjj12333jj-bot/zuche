package com.zuche.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增 / 编辑入参
 *
 * <p>状态允许为空：为空时按「草稿」处理。新写的公告直接推到用户面前，
 * 万一标题打错字或正文没写完，撤回来比发出去容易得多。</p>
 */
@Data
@Schema(description = "公告新增/编辑")
public class AnnouncementSaveDTO {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "标题最多 100 字")
    @Schema(description = "标题", example = "假期取车提醒")
    private String title;

    @NotBlank(message = "公告正文不能为空")
    @Size(max = 5000, message = "正文最多 5000 字")
    @Schema(description = "正文")
    private String content;

    @Pattern(regexp = "NOTICE|ACTIVITY|RULE", message = "类型只能是 NOTICE / ACTIVITY / RULE")
    @Schema(description = "类型：NOTICE 公告 / ACTIVITY 活动 / RULE 规则", example = "NOTICE")
    private String type;

    @Schema(description = "状态：1 直接发布 / 0 存草稿（默认）", example = "0")
    private Integer status;
}
