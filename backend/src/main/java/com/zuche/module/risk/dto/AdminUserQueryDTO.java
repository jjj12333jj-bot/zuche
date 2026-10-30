package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台用户信用查询
 */
@Data
@Schema(description = "用户信用查询参数")
public class AdminUserQueryDTO {

    @Schema(description = "用户名/姓名/手机号模糊搜索")
    private String keyword;

    @Schema(description = "信用等级 EXCELLENT/GOOD/NORMAL/WATCH")
    private String level;

    @Schema(description = "风险类型 WARNING/OVERRIDE/BLACKLIST，NONE 表示只查无风险用户")
    private String riskType;

    private Integer current = 1;

    private Integer pageSize = 10;
}
