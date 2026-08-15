package com.familykitchen.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求。
 
 * @param code 编码
 * @param mobile 手机号
 */
@Schema(description = "微信登录请求")
public record WechatLoginRequest(
    @Schema(description = "wx.login 返回的 code")
    @NotBlank String code,
    @Schema(description = "预留的手机号字段")
    String mobile
) {
}
