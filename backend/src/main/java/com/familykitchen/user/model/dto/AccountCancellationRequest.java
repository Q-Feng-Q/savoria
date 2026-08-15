package com.familykitchen.user.model.dto;
import jakarta.validation.constraints.NotBlank;
/** 账号注销申请，需要再次验证密码。 
 * @param password 密码
 */
public record AccountCancellationRequest(@NotBlank String password) {}
