package com.familykitchen.family.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 封装返回给调用方的家庭菜单项目数据。
 *
 * @param dishId 菜品标识
 * @param categoryId category标识
 * @param categoryName 分类名称
 * @param categorySortOrder 分类排序
 * @param dishName 菜品名称
 * @param description description
 * @param imageUrl imageUrl
 * @param basePrice basePrice
 * @param familyFinalPrice 家庭FinalPrice
 * @param enabled 是否启用
 * @param sortOrder sort订单
 * @param featuredAt 商户推荐时间
 * @param featured 是否为商户推荐菜
 */
public record FamilyMenuItemView(
    Long dishId,
    Long categoryId,
    String categoryName,
    Integer categorySortOrder,
    String dishName,
    String description,
    String imageUrl,
    BigDecimal basePrice,
    BigDecimal familyFinalPrice,
    boolean enabled,
    int sortOrder,
    LocalDateTime featuredAt,
    boolean featured
) {
}

