package com.familykitchen.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户修改密码请求。 
 * @param currentPassword 当前密码
 * @param newPassword new密码
 */
@Schema(description = "用户修改密码请求")
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 6, max = 64) String newPassword
) {}
