package com.familykitchen.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 绑定或更换邮箱请求。 
 * @param email 邮箱
 * @param verificationCode verification编码
 */
@Schema(description = "绑定或更换邮箱请求")
public record BindEmailRequest(@NotBlank @Email String email, @NotBlank String verificationCode) {}
