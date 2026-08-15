package com.familykitchen.purchase.model.bo;

import com.familykitchen.purchase.model.enums.IngredientCalcType;

import java.math.BigDecimal;

/**
 * 表示食材Demand领域计算过程中的业务数据。
 *
 * @param ingredientName 食材名称
 * @param quantity quantity
 * @param unit unit
 * @param calcType calc类型
 * @param dishQuantity 菜品Quantity
 */
public record IngredientDemand(
    String ingredientName,
    BigDecimal quantity,
    String unit,
    IngredientCalcType calcType,
    int dishQuantity
) {
}


