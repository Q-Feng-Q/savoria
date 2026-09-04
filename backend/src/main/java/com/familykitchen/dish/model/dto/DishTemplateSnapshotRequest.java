package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 模板可编辑业务字段的第二版完整审核快照。
 *
 * <p>图片、来源身份、模板类型和派生状态均由服务端维护，不属于该快照。</p>
 * @param schemaVersion 快照结构版本，固定为2
 * @param categoryId 模板分类ID
 * @param name 模板名称
 * @param description 可空模板简介
 * @param referencePrice 可空成品菜参考价格
 * @param tasteTags 口味标签
 * @param mealTags 推荐餐次
 * @param sortOrder 排序值
 * @param enabled 是否启用
 * @param ingredients 完整食材快照
 * @param cookingSteps 完整制作步骤快照
 */
@Schema(description = "模板可编辑业务字段第二版完整审核快照")
public record DishTemplateSnapshotRequest(Integer schemaVersion, Long categoryId, String name,
    String description, BigDecimal referencePrice, List<String> tasteTags, List<String> mealTags,
    Integer sortOrder, Boolean enabled, List<DishTemplateIngredientSnapshotRequest> ingredients,
    List<DishTemplateCookingStepSnapshotRequest> cookingSteps) { }
