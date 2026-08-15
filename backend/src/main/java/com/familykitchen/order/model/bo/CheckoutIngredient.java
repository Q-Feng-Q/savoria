package com.familykitchen.order.model.bo;

import com.familykitchen.purchase.model.enums.IngredientCalcType;

import java.math.BigDecimal;

/**
 * 表示Checkout食材领域计算过程中的业务数据。
 *
 * @param ingredientName 食材名称
 * @param quantity quantity
 * @param unit unit
 * @param calcType calc类型
 */
public record CheckoutIngredient(
        String ingredientName,
        BigDecimal quantity,
        String unit,
        IngredientCalcType calcType
) {
}

