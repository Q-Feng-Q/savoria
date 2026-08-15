package com.familykitchen.user.model.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
/** 请求发送邮箱绑定验证码。 
 * @param email 邮箱
 */
public record EmailCodeRequest(@NotBlank @Email String email) {}
