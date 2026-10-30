package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公告统计卡
 */
@Data
@Schema(description = "公告统计")
public class AnnouncementStatsVO {

    @Schema(description = "公告总数（不含已删除）")
    private Integer total;

    @Schema(description = "已发布")
    private Integer published;

    @Schema(description = "草稿")
    private Integer draft;

    @Schema(description = "累计浏览量")
    private Long viewTotal;
}
