package com.familykitchen.order.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 承载配送费用相关的请求参数。
 *
 * @param deliveryFee 配送费用
 */
public record DeliveryFeeRequest(
    @NotNull @DecimalMin("0.00") BigDecimal deliveryFee
) {
}
