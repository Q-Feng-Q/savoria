package com.familykitchen.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 用户名或邮箱密码登录请求。 
 * @param username 用户名
 * @param password 密码
 */
@Schema(description = "用户名或已验证邮箱密码登录请求")
public record UserLoginRequest(
    @NotBlank @Schema(description = "用户名或已验证邮箱") String username,
    @NotBlank @Schema(description = "登录密码") String password
) {}
