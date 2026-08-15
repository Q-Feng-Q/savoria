package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 承载Update商户家庭资料相关的请求参数。
 *
 * @param familyName 家庭名称
 * @param note note
 * @param contactNames 联系人Names
 */
public record UpdateMerchantFamilyProfileRequest(
    @NotBlank String familyName,
    String note,
    List<String> contactNames
) {
}

