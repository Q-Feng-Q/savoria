package com.familykitchen.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 对外返回的用户资料，不包含密码摘要或第三方原始标识。 
 * @param id 标识
 * @param username 用户名
 * @param nickname nickname
 * @param avatarUrl avatarUrl
 * @param mobile 手机号
 * @param maskedEmail masked邮箱
 * @param emailBound 邮箱Bound
 * @param wechatBound 微信Bound
 * @param status 状态
 */
@Schema(description = "用户个人资料")
public record UserProfileView(
    Long id, String username, String nickname, String avatarUrl, String mobile,
    String maskedEmail, boolean emailBound, boolean wechatBound, String status
) {}
