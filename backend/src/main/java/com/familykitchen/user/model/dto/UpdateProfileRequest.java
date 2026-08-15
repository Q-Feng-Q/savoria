package com.familykitchen.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户个人资料修改请求。 
 * @param nickname nickname
 * @param avatarUrl avatarUrl
 * @param mobile 手机号
 */
@Schema(description = "用户个人资料修改请求")
public record UpdateProfileRequest(
    @NotBlank @Size(max = 80) String nickname,
    @Size(max = 500) String avatarUrl,
    @Size(max = 32) String mobile
) {}
