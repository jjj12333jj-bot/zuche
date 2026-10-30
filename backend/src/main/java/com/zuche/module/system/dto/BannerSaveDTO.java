package com.zuche.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 轮播图新增 / 编辑入参
 */
@Data
@Schema(description = "轮播图新增/编辑")
public class BannerSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 80, message = "标题最多 80 字")
    @Schema(description = "标题", example = "信用越好 押金越低")
    private String title;

    @NotBlank(message = "图片地址不能为空")
    @Pattern(regexp = "^(/|https?://).+", message = "图片地址必须以 / 或 http(s):// 开头")
    @Size(max = 255, message = "图片地址最多 255 字符")
    @Schema(description = "图片地址：站内路径或以 http(s) 开头的外链", example = "/images/banners/banner-1.svg")
    private String imageUrl;

    @Pattern(regexp = "^$|^(/|https?://).+", message = "跳转地址必须以 / 或 http(s):// 开头，留空表示不可点击")
    @Size(max = 255, message = "跳转地址最多 255 字符")
    @Schema(description = "跳转地址，选填", example = "/credit")
    private String linkUrl;

    @Pattern(regexp = "HOME|PORTAL", message = "位置只能是 HOME / PORTAL")
    @Schema(description = "位置：HOME 首页 / PORTAL 列表页", example = "HOME")
    private String position;

    @Min(value = 0, message = "排序号不能为负数")
    @Schema(description = "排序号，同一位置内升序", example = "1")
    private Integer sortNo;

    @Schema(description = "状态：1 显示 / 0 隐藏", example = "1")
    private Integer status;
}
