package com.familykitchen.dish.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 平台管理员覆盖模板可编辑字段的第二版完整快照。
 * @param schemaVersion 快照结构版本
 * @param expectedVersion 期望模板版本
 * @param name 模板名称
 * @param description 模板简介
 * @param categoryId 模板分类ID
 * @param referencePrice 成品菜参考价格
 * @param tasteTags 口味标签
 * @param mealTags 推荐餐次
 * @param enabled 是否启用
 * @param ingredients 完整食材快照
 * @param cookingSteps 完整制作步骤快照
 * @param imageUrl 禁止提交的公共图片字段
 * @param imageSourceUrl 禁止提交的图片来源字段
 * @param imageAuthor 禁止提交的图片作者字段
 * @param imageLicense 禁止提交的图片许可证字段
 * @param sourceType 禁止提交的来源类型字段
 * @param sourceKey 禁止提交的来源键字段
 * @param sourceUrl 禁止提交的菜谱来源地址字段
 * @param sourceRevision 禁止提交的来源版本字段
 * @param sourceCategory 禁止提交的来源分类字段
 * @param sourceYieldText 禁止提交的来源份量字段
 * @param templateType 禁止提交的模板类型字段
 * @param dataStatus 禁止提交的数据状态字段
 * @param procurementReady 禁止提交的采购就绪字段
 * @param imageRightsStatus 禁止提交的图片权利状态字段
 */
@Schema(description = "平台模板完整编辑请求；不接受来源、图片和派生状态字段")
public record AdminDishTemplateUpdateRequest(
    @NotNull Integer schemaVersion,
    @NotNull Long expectedVersion,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 255) String description,
    @NotNull Long categoryId,
    @DecimalMin("0.00") BigDecimal referencePrice,
    @NotNull @Size(max = 10) List<@Size(max = 20) String> tasteTags,
    @NotNull @Size(max = 3) List<String> mealTags,
    @NotNull Boolean enabled,
    @NotNull @Size(max = 100) List<@Valid IngredientItem> ingredients,
    @NotNull @Size(max = 100) List<@Valid CookingStepItem> cookingSteps,
    String imageUrl, String imageSourceUrl, String imageAuthor, String imageLicense,
    String sourceType, String sourceKey, String sourceUrl, String sourceRevision,
    String sourceCategory, String sourceYieldText, String templateType, String dataStatus,
    Boolean procurementReady, String imageRightsStatus,
    String productType, String nourishmentDescription, String servingAdvice, String precautions
) {
  /** Backward-compatible constructor for clients without nourishment fields. */
  public AdminDishTemplateUpdateRequest(Integer schemaVersion,
    Long expectedVersion,
    String name,
    String description,
    Long categoryId,
    BigDecimal referencePrice,
    List<String> tasteTags,
    List<String> mealTags,
    Boolean enabled,
    List<IngredientItem> ingredients,
    List<CookingStepItem> cookingSteps,
    String imageUrl, String imageSourceUrl, String imageAuthor, String imageLicense,
    String sourceType, String sourceKey, String sourceUrl, String sourceRevision,
    String sourceCategory, String sourceYieldText, String templateType, String dataStatus,
    Boolean procurementReady, String imageRightsStatus) {
    this(schemaVersion, expectedVersion, name, description, categoryId, referencePrice, tasteTags, mealTags, enabled, ingredients, cookingSteps, imageUrl, imageSourceUrl, imageAuthor, imageLicense, sourceType, sourceKey, sourceUrl, sourceRevision, sourceCategory, sourceYieldText, templateType, dataStatus, procurementReady, imageRightsStatus, null, null, null, null);
  }


  /**
   * 创建不含任何服务端字段的正常编辑请求。
   * @param schemaVersion 快照结构版本
   * @param expectedVersion 期望模板版本
   * @param name 模板名称
   * @param description 模板简介
   * @param categoryId 模板分类ID
   * @param referencePrice 成品菜参考价格
   * @param tasteTags 口味标签
   * @param mealTags 推荐餐次
   * @param enabled 是否启用
   * @param ingredients 完整食材快照
   * @param cookingSteps 完整制作步骤快照
   */
  public AdminDishTemplateUpdateRequest(Integer schemaVersion, Long expectedVersion, String name,
      String description, Long categoryId, BigDecimal referencePrice, List<String> tasteTags,
      List<String> mealTags, Boolean enabled, List<IngredientItem> ingredients,
      List<CookingStepItem> cookingSteps) {
    this(schemaVersion, expectedVersion, name, description, categoryId, referencePrice, tasteTags,
        mealTags, enabled, ingredients, cookingSteps, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
  }

  /**
   * 模板快照中的稳定食材项。
   * @param itemId 客户端稳定项ID
   * @param ingredientName 食材名称
   * @param ingredientCategory 食材分类
   * @param quantityStatus 数量状态
   * @param quantity 家庭采购数量
   * @param unit 计量单位
   * @param calcType 用量计算方式
   * @param sourceText 来源原料说明
   * @param sourceQuantityText 来源批次用量
   * @param componentTemplateId 引用组件模板ID
   * @param componentMultiplier 组件展开倍数
   * @param sortOrder 排序值
   */
  public record IngredientItem(
      @NotBlank @Size(max = 500) String itemId,
      @NotBlank @Size(max = 100) String ingredientName,
      @NotBlank @Size(max = 50) String ingredientCategory,
      @NotBlank String quantityStatus,
      @DecimalMin(value = "0.01") BigDecimal quantity,
      @Size(max = 20) String unit,
      String calcType,
      @Size(max = 1000) String sourceText,
      @Size(max = 255) String sourceQuantityText,
      Long componentTemplateId,
      @DecimalMin(value = "0.0001") BigDecimal componentMultiplier,
      @NotNull Integer sortOrder) { }

  /**
   * 模板快照中的稳定制作步骤项。
   * @param itemId 客户端稳定项ID
   * @param stepNo 连续步骤序号
   * @param title 步骤标题
   * @param content 完整操作内容
   * @param durationSeconds 持续秒数
   * @param temperatureText 温度说明
   * @param heatLevel 火候说明
   * @param componentTemplateId 当前步骤引用的组件模板ID
   */
  public record CookingStepItem(
      @NotBlank @Size(max = 500) String itemId,
      @NotNull Integer stepNo,
      @Size(max = 100) String title,
      @NotBlank String content,
      Integer durationSeconds,
      @Size(max = 100) String temperatureText,
      @Size(max = 50) String heatLevel,
      Long componentTemplateId,
      @Size(max = 5) List<String> imageUrls) {
    public CookingStepItem(String itemId, Integer stepNo, String title, String content,
        Integer durationSeconds, String temperatureText, String heatLevel, Long componentTemplateId) {
      this(itemId, stepNo, title, content, durationSeconds, temperatureText, heatLevel, componentTemplateId, null);
    }
  }
}
