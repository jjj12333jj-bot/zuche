package com.zuche.module.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 信用明细查询条件
 */
@Data
@Schema(description = "信用明细查询")
public class CreditLogQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 10L;

    @Schema(description = "变动类型过滤 EVENT/BEHAVIOR/ADMIN/APPEAL，空为全部")
    private String changeType;
}
