package com.familykitchen.dish.model.vo;

/**
 * 封装返回给调用方的菜品Category数据。
 *
 * @param categoryId category标识
 * @param name 名称
 * @param sortOrder sort订单
 * @param enabled 是否启用
 */
public record DishCategoryView(
    Long categoryId,
    String name,
    int sortOrder,
    boolean enabled
) {
}

