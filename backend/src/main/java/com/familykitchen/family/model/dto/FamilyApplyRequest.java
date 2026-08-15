package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 承载家庭Apply相关的请求参数。
 *
 * @param familyName 家庭名称
 * @param merchantMode 商户Mode
 * @param merchantInvitationCode 商户邀请编码
 * @param newMerchantName new商户名称
 * @param contactName 联系人名称
 * @param contactPhone 联系人联系电话
 */
public record FamilyApplyRequest(
    @NotBlank @Size(max=100) String familyName,
    @NotBlank @Pattern(regexp="INVITATION|JOINT_CREATE|CREATE") String merchantMode,
    @Size(max=64) String merchantInvitationCode,
    @Size(max=100) String newMerchantName,
    @Size(max=50) String contactName,
    @Size(max=30) String contactPhone
) {}
