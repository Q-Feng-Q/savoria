package com.familykitchen.dish.model.entity;

import java.math.BigDecimal;

/**
 * 菜品食材持久化实体。
 */
public class DishIngredientEntity {

  /**
   * 标识。
   */
  private Long id;
  /**
   * 菜品标识。
   */
  private Long dishId;
  /**
   * 食材名称。
   */
  private String ingredientName;
  /**
   * quantity。
   */
  private BigDecimal quantity;
  /**
   * unit。
   */
  private String unit;
  /**
   * calc类型。
   */
  private String calcType;

  /**
   * 获取标识。
   *
   * @return 获取标识的结果
   */
  public Long getId() { return id; }
  /**
   * 标识。
   */
  /**
   * 设置标识。
   *
   * @param id 标识
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 获取菜品标识。
   *
   * @return 获取菜品标识的结果
   */
  public Long getDishId() { return dishId; }
  /**
   * 菜品标识。
   */
  /**
   * 设置菜品标识。
   *
   * @param dishId 菜品标识
   */
  public void setDishId(Long dishId) { this.dishId = dishId; }
  /**
   * 获取食材名称。
   *
   * @return 获取食材名称的结果
   */
  public String getIngredientName() { return ingredientName; }
  /**
   * 食材名称。
   */
  /**
   * 设置食材名称。
   *
   * @param ingredientName 食材名称
   */
  public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
  /**
   * 获取Quantity。
   *
   * @return 获取Quantity的结果
   */
  public BigDecimal getQuantity() { return quantity; }
  /**
   * quantity。
   */
  /**
   * 设置Quantity。
   *
   * @param quantity quantity
   */
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  /**
   * 获取Unit。
   *
   * @return 获取Unit的结果
   */
  public String getUnit() { return unit; }
  /**
   * unit。
   */
  /**
   * 设置Unit。
   *
   * @param unit unit
   */
  public void setUnit(String unit) { this.unit = unit; }
  /**
   * 获取Calc类型。
   *
   * @return 获取Calc类型的结果
   */
  public String getCalcType() { return calcType; }
  /**
   * calc类型。
   */
  /**
   * 设置Calc类型。
   *
   * @param calcType calc类型
   */
  public void setCalcType(String calcType) { this.calcType = calcType; }
}
