package com.familykitchen.dish.model.vo;

import java.util.List;

/**
 * 封装返回给调用方的食材Dictionary数据。
 *
 * @param ingredientId 食材标识
 * @param name 名称
 * @param category category
 * @param unit unit
 * @param referencedDishCount referenced菜品数量
 * @param referencedDishNames referenced菜品Names
 * @param removable removable
 */
public record IngredientDictionaryView(
    Long ingredientId,
    String name,
    String category,
    String unit,
    int referencedDishCount,
    List<String> referencedDishNames,
    boolean removable
) {
}

