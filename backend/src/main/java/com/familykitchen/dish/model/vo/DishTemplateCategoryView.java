package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 提供给商户端筛选器的平台菜品模板分类。
 * @param categoryId 模板分类 ID
 * @param code 稳定分类编码
 * @param name 分类名称
 * @param sortOrder 排序值
 */
@Schema(description = "平台菜品模板分类")
public record DishTemplateCategoryView(Long categoryId, String code, String name, Integer sortOrder) { }
