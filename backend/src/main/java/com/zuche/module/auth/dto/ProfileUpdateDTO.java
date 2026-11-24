package com.zuche.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料参数
 *
 * <p>只放开姓名、手机号、邮箱三个字段：用户名是登录凭据不允许改，
 * 信用分、角色、所属网点属于系统管理范围，也不在这里改。</p>
 */
@Data
@Schema(description = "修改个人资料参数")
public class ProfileUpdateDTO {

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
