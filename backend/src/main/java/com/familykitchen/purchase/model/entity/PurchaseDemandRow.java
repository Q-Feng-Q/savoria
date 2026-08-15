package com.familykitchen.purchase.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单采购需求查询行对象。
 */
public class PurchaseDemandRow {
  /**
   * 家庭标识。
   */
  private Long familyId;
  /**
   * mealSlot标识。
   */
  private Long mealSlotId;
  /**
   * 订单标识。
   */
  private Long orderId;
  /**
   * 菜品标识。
   */
  private Long dishId;
  /**
   * serviceDate。
   */
  private LocalDate serviceDate;
  /**
   * 订单状态。
   */
  private String orderStatus;
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
   * 菜品Quantity。
   */
  private Integer dishQuantity;

  /**
   * 获取家庭标识。
   *
   * @return 获取家庭标识的结果
   */
  public Long getFamilyId() { return familyId; }
  /**
   * 家庭标识。
   */
  /**
   * 设置家庭标识。
   *
   * @param familyId 家庭标识
   */
  public void setFamilyId(Long familyId) { this.familyId = familyId; }
  /**
   * 获取MealSlot标识。
   *
   * @return 获取MealSlot标识的结果
   */
  public Long getMealSlotId() { return mealSlotId; }
  /**
   * mealSlot标识。
   */
  /**
   * 设置MealSlot标识。
   *
   * @param mealSlotId mealSlot标识
   */
  public void setMealSlotId(Long mealSlotId) { this.mealSlotId = mealSlotId; }
  /**
   * 获取订单标识。
   *
   * @return 获取订单标识的结果
   */
  public Long getOrderId() { return orderId; }
  /**
   * 订单标识。
   */
  /**
   * 设置订单标识。
   *
   * @param orderId 订单标识
   */
  public void setOrderId(Long orderId) { this.orderId = orderId; }
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
   * 获取ServiceDate。
   *
   * @return 获取ServiceDate的结果
   */
  public LocalDate getServiceDate() { return serviceDate; }
  /**
   * serviceDate。
   */
  /**
   * 设置ServiceDate。
   *
   * @param serviceDate serviceDate
   */
  public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
  /**
   * 获取订单状态。
   *
   * @return 获取订单状态的结果
   */
  public String getOrderStatus() { return orderStatus; }
  /**
   * 订单状态。
   */
  /**
   * 设置订单状态。
   *
   * @param orderStatus 订单状态
   */
  public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
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
  /**
   * 获取菜品Quantity。
   *
   * @return 获取菜品Quantity的结果
   */
  public Integer getDishQuantity() { return dishQuantity; }
  /**
   * 菜品Quantity。
   */
  /**
   * 设置菜品Quantity。
   *
   * @param dishQuantity 菜品Quantity
   */
  public void setDishQuantity(Integer dishQuantity) { this.dishQuantity = dishQuantity; }
}
