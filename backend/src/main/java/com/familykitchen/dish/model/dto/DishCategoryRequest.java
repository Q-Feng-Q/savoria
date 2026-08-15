package com.familykitchen.dish.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 承载菜品Category相关的请求参数。
 *
 * @param name 名称
 * @param sortOrder sort订单
 * @param enabled 是否启用
 */
public record DishCategoryRequest(
    @NotBlank String name,
    int sortOrder,
    boolean enabled
) {
}

