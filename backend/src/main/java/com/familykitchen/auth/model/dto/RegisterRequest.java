package com.familykitchen.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 独立用户注册请求。 
 * @param username 用户名
 * @param password 密码
 * @param name 名称
 * @param mobile 手机号
 */
@Schema(description = "独立用户注册请求")
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_]+$") String username,
    @NotBlank @Size(min = 6, max = 64) String password,
    @NotBlank @Size(max = 80) String name,
    @Size(max = 32) String mobile
) {}
