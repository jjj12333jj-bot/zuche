package com.zuche.module.system.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轮播图查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "轮播图查询参数")
public class BannerQueryDTO extends PageQuery {

    @Schema(description = "标题，模糊匹配")
    private String keyword;

    @Schema(description = "位置：HOME 首页 / PORTAL 列表页，为空查全部")
    private String position;

    @Schema(description = "状态：1 显示 / 0 隐藏，为空查全部")
    private Integer status;
}
