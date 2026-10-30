package com.zuche.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台用户统计
 */
@Data
@Schema(description = "平台用户统计")
public class AdminUserStatsVO {

    private Integer total;

    @Schema(description = "正常账号")
    private Integer normal;

    @Schema(description = "已冻结")
    private Integer frozen;

    @Schema(description = "普通用户（可下单）")
    private Integer customers;

    @Schema(description = "网点管理员")
    private Integer storeAdmins;

    @Schema(description = "本月新增")
    private Integer monthNew;
}
