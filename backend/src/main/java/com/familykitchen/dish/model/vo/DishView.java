package com.familykitchen.dish.model.vo;

import java.math.BigDecimal;

/**
 * 封装返回给调用方的菜品数据。
 *
 * @param dishId 菜品标识
 * @param categoryId category标识
 * @param name 名称
 * @param description description
 * @param imageUrl imageUrl
 * @param price price
 * @param status 状态
 */
public record DishView(
    Long dishId,
    Long categoryId,
    String name,
    String description,
    String imageUrl,
    BigDecimal price,
    String status
) {
}

