package com.familykitchen.dish.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 承载食材Dictionary相关的请求参数。
 *
 * @param name 名称
 * @param category category
 * @param unit unit
 */
public record IngredientDictionaryRequest(
    @NotBlank String name,
    @NotBlank String category,
    @NotBlank String unit
) {
}

