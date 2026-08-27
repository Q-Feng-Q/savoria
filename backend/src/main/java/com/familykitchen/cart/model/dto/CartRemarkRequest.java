package com.familykitchen.cart.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 承载购物车备注相关的请求参数。
 *
 * @param cartId cart identifier
 * @param cartVersion expected cart version
 * @param requestId durable request identifier
 * @param remark 备注
 */
public record CartRemarkRequest(
    @NotNull Long cartId,
    @NotNull @Min(0) Long cartVersion,
    @NotBlank String requestId,
    String remark
) {
}

