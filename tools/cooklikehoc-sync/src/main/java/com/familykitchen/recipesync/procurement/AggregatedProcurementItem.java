package com.familykitchen.recipesync.procurement;

import java.math.BigDecimal;

/** Compatible verified leaves combined into one procurement quantity. */
public record AggregatedProcurementItem(
    String ingredientName,
    BigDecimal quantity,
    String unit,
    String calcType) {
}
