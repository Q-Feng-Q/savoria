package com.familykitchen.admin.model.dto;

/**
 * 承载平台管理用户角色相关的请求参数。
 *
 * @param platformAdmin platform平台管理
 */
public record AdminUserRoleRequest(boolean platformAdmin) {}
