package com.familykitchen.recipesync.config;

import java.math.BigDecimal;

/** Legacy procurement row retained for deterministic source merging. */
public record BaselineIngredient(
    String ingredientName,
    String ingredientCategory,
    BigDecimal quantity,
    String unit,
    String calcType,
    int sortOrder) {
}
