package com.familykitchen.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 后台账号登录请求。
 
 * @param username 用户名
 * @param password 密码
 */
@Schema(description = "后台账号登录请求")
public record AdminLoginRequest(
    @Schema(description = "登录用户名", example = "admin")
    @NotBlank String username,
    @Schema(description = "登录密码", example = "123456")
    @NotBlank String password
) {
}
