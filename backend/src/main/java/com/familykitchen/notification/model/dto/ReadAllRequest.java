package com.familykitchen.notification.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 承载ReadAll相关的请求参数。
 *
 * @param receiverScope receiverScope
 */
public record ReadAllRequest(@NotBlank String receiverScope) {
}

