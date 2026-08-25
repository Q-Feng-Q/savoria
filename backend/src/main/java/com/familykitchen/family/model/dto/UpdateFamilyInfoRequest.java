package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 承载家庭端修改家庭资料相关的请求参数。
 *
 * @param familyName 家庭名称
 * @param note 备注
 */
public record UpdateFamilyInfoRequest(
    @NotBlank @Size(max = 100) String familyName,
    @Size(max = 255) String note
) {
}
