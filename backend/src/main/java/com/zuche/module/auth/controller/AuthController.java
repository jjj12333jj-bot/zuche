package com.zuche.module.auth.controller;

import com.zuche.common.Result;
import com.zuche.module.auth.dto.LoginDTO;
import com.zuche.module.auth.dto.RegisterDTO;
import com.zuche.module.auth.service.AuthService;
import com.zuche.module.auth.vo.LoginVO;
import com.zuche.module.system.service.MenuService;
import com.zuche.module.system.vo.MenuVO;
import com.zuche.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录、注册、当前用户、菜单树
 */
@Tag(name = "01-认证", description = "登录、注册、当前用户、动态菜单")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MenuService menuService;

    @Operation(summary = "登录", description = "返回 JWT 与角色码，前端据此渲染菜单")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @Operation(summary = "注册", description = "仅普通用户；注册成功直接返回 token，前端可免登录进入首页")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    @Operation(summary = "当前登录用户", security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/me")
    public Result<LoginVO> me() {
        return Result.success(authService.currentUser(UserContext.requireUserId()));
    }

    @Operation(summary = "当前用户菜单树", description = "按角色返回 M/C 类型菜单，前端侧边栏动态渲染",
            security = @SecurityRequirement(name = "Authorization"))
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.success(menuService.menuTree(UserContext.requireUserId()));
    }
}
