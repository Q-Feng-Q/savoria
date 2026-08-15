package com.familykitchen.admin.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 承载平台管理家庭Update相关的请求参数。
 *
 * @param familyName 家庭名称
 * @param note note
 * @param status 状态
 * @param deliveryEnabled 配送是否启用
 * @param deliveryFeeDefault 配送费用Default
 * @param deliveryFree 配送Free
 */
public record AdminFamilyUpdateRequest(
    @NotBlank String familyName,
    String note,
    @NotBlank String status,
    @NotNull Boolean deliveryEnabled,
    @NotNull @DecimalMin("0.00") BigDecimal deliveryFeeDefault,
    @NotNull Boolean deliveryFree
) {
}
