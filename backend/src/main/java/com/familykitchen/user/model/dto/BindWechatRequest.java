package com.familykitchen.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 绑定微信请求，authorizationCode 为微信临时登录码。 
 * @param authorizationCode authorization编码
 */
@Schema(description = "绑定微信请求")
public record BindWechatRequest(@NotBlank String authorizationCode) {}
