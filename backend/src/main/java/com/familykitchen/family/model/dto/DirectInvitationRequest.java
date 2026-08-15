package com.familykitchen.family.model.dto;
import jakarta.validation.constraints.NotBlank;
/** 按用户名或邮箱发起定向家庭邀请。 
 * @param userIdentifier 用户Identifier
 */
public record DirectInvitationRequest(@NotBlank String userIdentifier) {}
