package com.familykitchen.dish.model.vo;

import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.entity.DishTemplateNameAliasEntity;
import com.familykitchen.dish.model.entity.DishTemplateSourceRecordEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 平台管理员可见的模板完整详情，不包含任何文件系统路径。
 * @param templateId 模板ID
 * @param templateCode 稳定模板编码
 * @param categoryId 模板分类ID
 * @param categoryName 模板分类名称
 * @param name 模板名称
 * @param description 模板简介
 * @param imageUrl 已发布公共图片地址
 * @param referencePrice 参考价格
 * @param tasteTags 口味标签
 * @param mealTags 推荐餐次
 * @param sourceType 来源类型
 * @param sourceCategory 来源分类
 * @param templateType 模板类型
 * @param dataStatus 数据完整状态
 * @param procurementReady 采购用量是否就绪
 * @param imageRightsStatus 图片权利状态
 * @param sortOrder 排序值
 * @param enabled 是否启用
 * @param version 并发版本号
 * @param sourceRecords 来源记录
 * @param nameAliases 名称别名
 * @param ingredients 完整食材图
 * @param cookingSteps 有序制作步骤
 * @param internalAssetReviews 内部图片审核摘要
 */
@Schema(description = "平台模板管理详情")
public record AdminDishTemplateDetailView(Long templateId, String templateCode, Long categoryId,
    String categoryName, String name, String description, String imageUrl, BigDecimal referencePrice,
    List<String> tasteTags, List<String> mealTags, String sourceType, String sourceCategory,
    String templateType, String dataStatus, boolean procurementReady, String imageRightsStatus,
    Integer sortOrder, boolean enabled, Long version, List<DishTemplateSourceRecordEntity> sourceRecords,
    List<DishTemplateNameAliasEntity> nameAliases, List<DishTemplateIngredientEntity> ingredients,
    List<DishTemplateCookingStepEntity> cookingSteps, List<InternalAssetReview> internalAssetReviews,
    String productType, String nourishmentDescription, String servingAdvice, String precautions
) {
  /** Backward-compatible constructor for clients without nourishment fields. */
  public AdminDishTemplateDetailView(Long templateId, String templateCode, Long categoryId,
    String categoryName, String name, String description, String imageUrl, BigDecimal referencePrice,
    List<String> tasteTags, List<String> mealTags, String sourceType, String sourceCategory,
    String templateType, String dataStatus, boolean procurementReady, String imageRightsStatus,
    Integer sortOrder, boolean enabled, Long version, List<DishTemplateSourceRecordEntity> sourceRecords,
    List<DishTemplateNameAliasEntity> nameAliases, List<DishTemplateIngredientEntity> ingredients,
    List<DishTemplateCookingStepEntity> cookingSteps, List<InternalAssetReview> internalAssetReviews) {
    this(templateId, templateCode, categoryId, categoryName, name, description, imageUrl, referencePrice, tasteTags, mealTags, sourceType, sourceCategory, templateType, dataStatus, procurementReady, imageRightsStatus, sortOrder, enabled, version, sourceRecords, nameAliases, ingredients, cookingSteps, internalAssetReviews, "NORMAL", null, null, null);
  }

  /**
   * 内部图片审核摘要，只暴露受控资源ID。
   * @param assetId 受控图片资源ID
   * @param assetStatus 审核状态
   * @param previewAvailable 是否允许受控预览
   * @param mimeType 图片媒体类型
   * @param fileSize 文件字节数
   * @param rejectionReason 驳回原因
   */
  public record InternalAssetReview(Long assetId, String assetStatus, boolean previewAvailable,
      String mimeType, Long fileSize, String rejectionReason) { }
}
