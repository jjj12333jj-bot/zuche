package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台申诉查询
 */
@Data
@Schema(description = "申诉查询参数")
public class AppealQueryDTO {

    @Schema(description = "PENDING / APPROVED / REJECTED，为空查全部")
    private String status;

    @Schema(description = "申诉单号/用户名/姓名模糊搜索")
    private String keyword;

    private Integer current = 1;

    private Integer pageSize = 10;
}
