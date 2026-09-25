package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 模板可编辑业务字段的第二版完整审核快照。
 *
 * <p>图片只能引用当前用户已上传的文件资产；图片来源身份、模板类型和派生状态仍由服务端维护。</p>
 * @param schemaVersion 快照结构版本，固定为2
 * @param categoryId 模板分类ID
 * @param name 模板名称
 * @param description 可空模板简介
 * @param imageUrl 目标图片地址；旧客户端未提交时由服务端保留原图
 * @param imageAssetId 本次新上传图片的文件资产ID，未更换图片时为空
 * @param removeImage 是否申请删除当前图片
 * @param imageRightsConfirmed 是否确认拥有新上传图片的合法使用权
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
    String description, String imageUrl, Long imageAssetId, Boolean removeImage,
    Boolean imageRightsConfirmed, BigDecimal referencePrice, List<String> tasteTags, List<String> mealTags,
    Integer sortOrder, Boolean enabled, List<DishTemplateIngredientSnapshotRequest> ingredients,
    List<DishTemplateCookingStepSnapshotRequest> cookingSteps,
    String productType, String nourishmentDescription, String servingAdvice, String precautions
) {
  /** Backward-compatible constructor for clients without nourishment fields. */
  public DishTemplateSnapshotRequest(Integer schemaVersion, Long categoryId, String name,
    String description, String imageUrl, Long imageAssetId, Boolean removeImage,
    Boolean imageRightsConfirmed, BigDecimal referencePrice, List<String> tasteTags, List<String> mealTags,
    Integer sortOrder, Boolean enabled, List<DishTemplateIngredientSnapshotRequest> ingredients,
    List<DishTemplateCookingStepSnapshotRequest> cookingSteps) {
    this(schemaVersion, categoryId, name, description, imageUrl, imageAssetId, removeImage, imageRightsConfirmed, referencePrice, tasteTags, mealTags, sortOrder, enabled, ingredients, cookingSteps, null, null, null, null);
  }
 }
