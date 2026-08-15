package com.familykitchen.cart.model.entity;

import java.math.BigDecimal;

/**
 * 可加入餐篮的菜品快照。
 
 * @param dishId 菜品标识
 * @param dishName 菜品名称
 * @param price price
 */
public record CartDishSnapshot(
    Long dishId,
    String dishName,
    BigDecimal price
) {
}
