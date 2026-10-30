package com.zuche.module.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 品牌新增 / 编辑
 */
@Data
@Schema(description = "品牌新增或编辑")
public class BrandSaveDTO {

    @NotBlank(message = "品牌名不能为空")
    @Size(max = 30, message = "品牌名最长 30 个字")
    private String brandName;

    @Schema(description = "品牌 Logo 地址，沿用本地 /images/brands/*.svg")
    @Size(max = 255, message = "Logo 地址过长")
    private String brandLogo;

    @Schema(description = "首字母，用于前台品牌索引；不填时取品牌名拼音首字母位置兜底")
    @Size(max = 4, message = "首字母最长 4 个字符")
    private String initial;

    @Schema(description = "排序号，越小越靠前")
    @Min(value = 0, message = "排序号不能为负")
    @Max(value = 9999, message = "排序号不能超过 9999")
    private Integer sortNo;

    @Schema(description = "1 启用 0 停用")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
