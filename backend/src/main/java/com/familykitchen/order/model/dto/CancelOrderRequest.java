package com.familykitchen.order.model.dto;

/**
 * 承载Cancel订单相关的请求参数。
 *
 * @param reason 原因
 */
public record CancelOrderRequest(String reason) {
}
