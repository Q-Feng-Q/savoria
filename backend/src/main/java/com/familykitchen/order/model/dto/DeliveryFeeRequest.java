package com.familykitchen.order.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 承载配送费用相关的请求参数。
 *
 * @param deliveryFee 配送费用
 * @param requestId 本次调价命令的幂等请求号
 */
public record DeliveryFeeRequest(
    @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal deliveryFee,
    @NotBlank @Size(max = 64) String requestId
) {
}
