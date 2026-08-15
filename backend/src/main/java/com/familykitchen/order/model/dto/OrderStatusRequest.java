package com.familykitchen.order.model.dto;

import com.familykitchen.order.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 承载订单状态相关的请求参数。
 *
 * @param status 状态
 * @param reason 原因
 */
public record OrderStatusRequest(
    @NotNull OrderStatus status,
    String reason
) {
}

