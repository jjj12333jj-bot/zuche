package com.zuche.module.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台风控名单查询
 */
@Data
@Schema(description = "风控名单查询参数")
public class BlacklistQueryDTO {

    @Schema(description = "ACTIVE / RELEASED，为空查全部")
    private String status;

    @Schema(description = "WARNING / OVERRIDE / BLACKLIST")
    private String listType;

    @Schema(description = "用户名/姓名/手机号模糊搜索")
    private String keyword;

    private Integer current = 1;

    private Integer pageSize = 10;
}
