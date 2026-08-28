package com.familykitchen.order.model.dto;

import com.familykitchen.order.model.enums.DeliveryMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 承载Submit订单相关的请求参数。
 *
 * @param cartId shared cart identifier
 * @param cartVersion expected cart version
 * @param requestId client idempotency key
 * @param deliveryMode 配送Mode
 * @param addressId 地址标识
 * @param remark 备注
 */
public record SubmitOrderRequest(
    @NotNull Long cartId,
    @NotNull Long cartVersion,
    @NotBlank String requestId,
    @NotNull DeliveryMode deliveryMode,
    Long addressId,
    String remark
) {
}

