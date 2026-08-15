package com.familykitchen.family.model.entity;

/**
 * 餐次查询行对象。
 */
public class MealSlotRecord {
  /**
   * mealSlot标识。
   */
  private Long mealSlotId;
  /**
   * 名称。
   */
  private String name;
  /**
   * display时间。
   */
  private String displayTime;

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
   * 获取名称。
   *
   * @return 获取名称的结果
   */
  public String getName() { return name; }
  /**
   * 名称。
   */
  /**
   * 设置名称。
   *
   * @param name 名称
   */
  public void setName(String name) { this.name = name; }
  /**
   * 获取Display时间。
   *
   * @return 获取Display时间的结果
   */
  public String getDisplayTime() { return displayTime; }
  /**
   * display时间。
   */
  /**
   * 设置Display时间。
   *
   * @param displayTime display时间
   */
  public void setDisplayTime(String displayTime) { this.displayTime = displayTime; }
}
