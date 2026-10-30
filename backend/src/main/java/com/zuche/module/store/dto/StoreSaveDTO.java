package com.zuche.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 网点新增/编辑
 *
 * <p>不含车辆数字段：车辆数随调车实时变化，存冗余字段必然对不上，
 * 一律用 COUNT(vehicle) 现算（这条约定写在 Store 实体的类注释里）。</p>
 */
@Data
@Schema(description = "网点保存参数")
public class StoreSaveDTO {

    @Schema(description = "网点编码，如 S005，创建后不建议改", example = "S005")
    @NotBlank(message = "网点编码不能为空")
    @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "网点编码只能是 2~10 位大写字母或数字，如 S005")
    private String storeCode;

    @Schema(description = "网点名称", example = "西二旗地铁店")
    @NotBlank(message = "网点名称不能为空")
    @Size(max = 50, message = "网点名称最多 50 字")
    private String storeName;

    @Schema(description = "详细地址")
    @NotBlank(message = "地址不能为空")
    @Size(max = 120, message = "地址最多 120 字")
    private String address;

    @Schema(description = "联系电话")
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^0\\d{2,3}-?\\d{7,8}$|^1[3-9]\\d{9}$",
            message = "电话格式不正确，座机如 010-88886666，手机如 13800138000")
    private String phone;

    @Schema(description = "营业时间", example = "08:00-22:00")
    @NotBlank(message = "营业时间不能为空")
    @Size(max = 50, message = "营业时间最多 50 字")
    private String businessHours;

    @Schema(description = "经度，仅存储，不做地图渲染")
    private BigDecimal longitude;

    @Schema(description = "纬度，仅存储，不做地图渲染")
    private BigDecimal latitude;

    @Schema(description = "负责人（网点管理员用户 ID），可空")
    private Long managerId;

    @Schema(description = "1 营业 / 0 停用", example = "1")
    private Integer status = 1;
}
