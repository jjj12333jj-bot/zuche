package com.zuche.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码参数
 *
 * <p>必须校验原密码：token 有效不代表本人当前在操作（比如公用电脑没退出），
 * 只凭 token 就能改密码等于把账号拱手让人。</p>
 */
@Data
@Schema(description = "修改密码参数")
public class PasswordChangeDTO {

    @NotBlank(message = "请输入原密码")
    @Schema(description = "原密码", example = "123456")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需在 6-32 位之间")
    @Schema(description = "新密码", example = "abc123456")
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    @Schema(description = "确认新密码", example = "abc123456")
    private String confirmPassword;
}
