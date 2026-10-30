package com.zuche.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册参数
 *
 * <p>注册只开放普通用户（USER）角色；网点管理员与系统管理员由后台创建。
 * 新用户信用分固定 100（良好档），押金系数 0.9，后续由租赁行为动态调整。</p>
 */
@Data
@Schema(description = "注册参数")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$",
            message = "用户名需以字母开头，4-20 位字母/数字/下划线")
    @Schema(description = "登录账号", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    @Schema(description = "登录密码", example = "123456")
    private String password;

    @NotBlank(message = "请再次输入密码")
    @Schema(description = "确认密码", example = "123456")
    private String confirmPassword;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名过长")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13900001234")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱过长")
    @Schema(description = "邮箱（选填）", example = "zhangsan@example.com")
    private String email;
}
