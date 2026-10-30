package com.zuche.module.user.dto;

import com.zuche.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台用户查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台用户查询参数")
public class AdminUserQueryDTO extends PageQuery {

    @Schema(description = "用户名 / 姓名 / 手机号 / 邮箱，模糊匹配")
    private String keyword;

    @Schema(description = "账号状态：1 正常 / 0 已冻结，为空查全部")
    private Integer status;

    @Schema(description = "角色码 USER / STORE_ADMIN / SYS_ADMIN，为空查全部")
    private String roleCode;

    @Schema(description = "信用等级 EXCELLENT / GOOD / NORMAL / WATCH")
    private String level;
}
