package com.zuche.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录返回：token + 前端渲染所需的最小用户信息
 */
@Data
@Builder
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "JWT，请求头格式 Authorization: Bearer <token>")
    private String token;

    private Long userId;
    private String username;
    private String realName;
    private String avatar;

    @Schema(description = "角色码 USER/STORE_ADMIN/SYS_ADMIN，前端据此渲染菜单")
    private String roleCode;

    @Schema(description = "信用分（仅普通用户有意义）")
    private Integer creditScore;

    @Schema(description = "信用等级")
    private String creditLevel;

    @Schema(description = "所属网点（网点管理员）")
    private Long storeId;
}
