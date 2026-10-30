package com.zuche.module.system;

import com.zuche.common.Result;
import com.zuche.security.LoginUser;
import com.zuche.security.RequireRole;
import com.zuche.security.RoleCode;
import com.zuche.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统自检接口
 *
 * <p>S0 阶段用它验证三件事：公开接口可访问、JWT 生效、角色拦截生效。
 * 后续阶段可保留 health 用于部署探活。</p>
 */
@Tag(name = "00-系统", description = "健康检查与权限自检")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Operation(summary = "健康检查（公开）")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "zuche-backend");
        data.put("status", "UP");
        data.put("time", LocalDateTime.now());
        return Result.success(data);
    }

    @Operation(summary = "登录自检：任意角色可访问", security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/whoami")
    public Result<LoginUser> whoami() {
        return Result.success(UserContext.get());
    }

    @Operation(summary = "权限自检：仅系统管理员可访问", security = @SecurityRequirement(name = "Authorization"))
    @RequireRole(RoleCode.SYS_ADMIN)
    @GetMapping("/admin/ping")
    public Result<String> adminPing() {
        return Result.success("pong: 你是系统管理员");
    }

    @Operation(summary = "权限自检：仅网点管理员可访问", security = @SecurityRequirement(name = "Authorization"))
    @RequireRole(RoleCode.STORE_ADMIN)
    @GetMapping("/store/ping")
    public Result<String> storePing() {
        return Result.success("pong: 你是网点管理员");
    }
}
