package com.familykitchen.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 承载平台管理用户Create相关的请求参数。
 *
 * @param username 用户名
 * @param password 密码
 * @param name 名称
 * @param mobile 手机号
 * @param platformAdmin platform平台管理
 */
public record AdminUserCreateRequest(
    @NotBlank @Size(min=3,max=50) @Pattern(regexp="^[A-Za-z0-9_]+$") String username,
    @NotBlank @Size(min=6,max=64) String password,
    @NotBlank @Size(max=80) String name,
    @Size(max=32) String mobile,
    boolean platformAdmin) {}
