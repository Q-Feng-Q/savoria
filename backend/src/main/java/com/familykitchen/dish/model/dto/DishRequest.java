package com.familykitchen.dish.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    String status
) {

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
   */
  public record CookingStepRequest(
      @Min(1) int stepNo,
      String title,
      @NotBlank String content
  ) {
  }
}

