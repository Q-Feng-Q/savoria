package com.familykitchen.auth.model.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
/** 请求发送密码找回验证码。 
 * @param email 邮箱
 */
public record PasswordResetCodeRequest(@NotBlank @Email String email) {}
