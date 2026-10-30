package com.zuche.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图列表项
 */
@Data
@Schema(description = "轮播图列表项")
public class BannerVO {

    private Long id;

    private String title;

    private String imageUrl;

    private String linkUrl;

    @Schema(description = "HOME / PORTAL")
    private String position;

    @Schema(description = "位置中文名")
    private String positionLabel;

    private Integer sortNo;

    private Integer status;

    @Schema(description = "状态中文名：显示 / 隐藏")
    private String statusLabel;

    private LocalDateTime createTime;
}
