package com.familykitchen.family.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 通过邀请码加入家庭请求。
 
 * @param code 编码
 */
@Schema(description = "通过邀请码加入家庭请求")
public record JoinFamilyRequest(
    @Schema(description = "邀请码")
    @NotBlank String code
) {}
