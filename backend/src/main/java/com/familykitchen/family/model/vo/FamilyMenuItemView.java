package com.familykitchen.family.model.vo;

import java.math.BigDecimal;

/**
 * 封装返回给调用方的家庭菜单项目数据。
 *
 * @param dishId 菜品标识
 * @param categoryId category标识
 * @param dishName 菜品名称
 * @param description description
 * @param imageUrl imageUrl
 * @param basePrice basePrice
 * @param familyFinalPrice 家庭FinalPrice
 * @param enabled 是否启用
 * @param sortOrder sort订单
 */
public record FamilyMenuItemView(
    Long dishId,
    Long categoryId,
    String dishName,
    String description,
    String imageUrl,
    BigDecimal basePrice,
    BigDecimal familyFinalPrice,
    boolean enabled,
    int sortOrder
) {
}

