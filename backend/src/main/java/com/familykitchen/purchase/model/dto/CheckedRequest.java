package com.familykitchen.purchase.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 承载Checked相关的请求参数。
 *
 * @param checked checked
 */
public record CheckedRequest(@NotNull Boolean checked) {
}

