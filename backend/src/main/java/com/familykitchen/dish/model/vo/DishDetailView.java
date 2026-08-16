package com.familykitchen.dish.model.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 封装返回给调用方的菜品详情数据。
 *
 * @param dishId 菜品标识
 * @param categoryId category标识
 * @param name 名称
 * @param description description
 * @param imageUrl imageUrl
 * @param price price
 * @param status 状态
 * @param sourceTemplateId 来源平台模板 ID，手工菜品为空
 * @param templateImported 是否由平台模板导入
 * @param ingredients ingredients
 * @param cookingSteps cookingSteps
 */
public record DishDetailView(
    Long dishId,
    Long categoryId,
    String name,
    String description,
    String imageUrl,
    BigDecimal price,
    String status,
    Long sourceTemplateId,
    boolean templateImported,
    List<IngredientView> ingredients,
    List<CookingStepView> cookingSteps
) {

  /**
   * 封装返回给调用方的食材数据。
   *
   * @param ingredientName 食材名称
   * @param quantity quantity
   * @param unit unit
   * @param calcType calc类型
   */
  public record IngredientView(
      String ingredientName,
      BigDecimal quantity,
      String unit,
      String calcType
  ) {
  }

  /**
   * 封装返回给调用方的CookingStep数据。
   *
   * @param stepNo stepNo
   * @param title title
   * @param content content
   */
  public record CookingStepView(
      int stepNo,
      String title,
      String content
  ) {
  }
}

