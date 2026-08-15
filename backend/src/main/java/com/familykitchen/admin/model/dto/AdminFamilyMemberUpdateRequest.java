package com.familykitchen.admin.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 承载平台管理家庭成员Update相关的请求参数。
 *
 * @param name 名称
 * @param mobile 手机号
 * @param roleTemplate 角色Template
 * @param activationStatus activation状态
 */
public record AdminFamilyMemberUpdateRequest(
    @NotBlank String name,
    String mobile,
    @NotBlank String roleTemplate,
    @NotBlank String activationStatus
) {
}
