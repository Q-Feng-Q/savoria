package com.familykitchen.admin.model.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 承载平台管理用户状态相关的请求参数。
 *
 * @param status 状态
 */
public record AdminUserStatusRequest(@Pattern(regexp = "ACTIVE|FROZEN") String status) {}
