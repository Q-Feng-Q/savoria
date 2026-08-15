package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 承载Update家庭配送Policy相关的请求参数。
 *
 * @param deliveryEnabled 配送是否启用
 * @param deliveryFeeDefault 配送费用Default
 * @param deliveryFree 配送Free
 */
public record UpdateFamilyDeliveryPolicyRequest(
    @NotNull Boolean deliveryEnabled,
    @NotNull @DecimalMin("0.00") BigDecimal deliveryFeeDefault,
    @NotNull Boolean deliveryFree
) {
}

