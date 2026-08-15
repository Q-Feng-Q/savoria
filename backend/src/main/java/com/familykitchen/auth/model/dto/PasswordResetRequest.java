package com.familykitchen.auth.model.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/** 邮箱验证码重置密码请求。 
 * @param email 邮箱
 * @param code 编码
 * @param newPassword new密码
 */
public record PasswordResetRequest(@NotBlank @Email String email, @NotBlank String code,
                                   @NotBlank @Size(min=6,max=64) String newPassword) {}
