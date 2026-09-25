package com.familykitchen.dish.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 承载菜品相关的请求参数。
 *
 * @param name 名称
 * @param categoryId category标识
 * @param description description
 * @param imageUrl imageUrl
 * @param basePrice basePrice
 * @param ingredients ingredients
 * @param cookingSteps cookingSteps
 * @param status 状态
 */
public record DishRequest(
    @NotBlank String name,
    @NotNull Long categoryId,
    String description,
    String imageUrl,
    @NotNull @DecimalMin("0.00") BigDecimal basePrice,
    List<@Valid IngredientRequest> ingredients,
    List<@Valid CookingStepRequest> cookingSteps,
    String status,
    String productType, String nourishmentDescription, String servingAdvice, String precautions
) {
  /** Backward-compatible constructor for clients without nourishment fields. */
  public DishRequest(String name,
    Long categoryId,
    String description,
    String imageUrl,
    BigDecimal basePrice,
    List<IngredientRequest> ingredients,
    List<CookingStepRequest> cookingSteps,
    String status) {
    this(name, categoryId, description, imageUrl, basePrice, ingredients, cookingSteps, status, null, null, null, null);
  }


  /**
   * 承载食材相关的请求参数。
   *
   * @param ingredientName 食材名称
   * @param quantity quantity
   * @param unit unit
   * @param calcType calc类型
   */
  public record IngredientRequest(
      @NotBlank String ingredientName,
      @NotNull @DecimalMin("0.00") BigDecimal quantity,
      @NotBlank String unit,
      @NotBlank @Pattern(regexp = "FIXED|PER_PERSON|NO_PURCHASE") String calcType
  ) {
  }

  /**
   * 承载CookingStep相关的请求参数。
   *
   * @param stepNo stepNo
   * @param title title
   * @param content content
   * @param durationSeconds 制作持续秒数；未知时为空
   * @param temperatureText 温度说明；未知时为空
   * @param heatLevel 火候说明；未知时为空
   * @param componentTemplateId 当前步骤引用的配料组件模板ID；未引用时为空
   */
  public record CookingStepRequest(
      @Min(1) int stepNo,
      @Size(max = 100) String title,
      @NotBlank String content,
      @Min(0) Integer durationSeconds,
      @Size(max = 100) String temperatureText,
      @Size(max = 50) String heatLevel,
      Long componentTemplateId,
      @Size(max = 5) List<String> imageUrls
  ) {
    public CookingStepRequest(int stepNo, String title, String content, Integer durationSeconds,
        String temperatureText, String heatLevel, Long componentTemplateId) {
      this(stepNo, title, content, durationSeconds, temperatureText, heatLevel, componentTemplateId, null);
    }

    /**
     * 兼容只维护基础步骤文本的调用方。
     *
     * @param stepNo 步骤序号
     * @param title 步骤标题
     * @param content 步骤内容
     */
    public CookingStepRequest(int stepNo, String title, String content) {
      this(stepNo, title, content, null, null, null, null);
    }
  }
}

