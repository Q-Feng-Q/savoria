package com.familykitchen.purchase.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 临时采购项持久化实体。
 */
public class TempPurchaseItemEntity {
  /**
   * 标识。
   */
  private Long id;
  /**
   * 商户标识。
   */
  private Long merchantId;
  /**
   * serviceDate。
   */
  private LocalDate serviceDate;
  /**
   * mealSlot标识。
   */
  private Long mealSlotId;
  /**
   * 家庭标识。
   */
  private Long familyId;
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
   * 备注。
   */
  private String remark;
  /**
   * checked。
   */
  private Boolean checked;

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
   * 获取商户标识。
   *
   * @return 获取商户标识的结果
   */
  public Long getMerchantId() { return merchantId; }
  /**
   * 商户标识。
   */
  /**
   * 设置商户标识。
   *
   * @param merchantId 商户标识
   */
  public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
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
   * 获取备注。
   *
   * @return 获取备注的结果
   */
  public String getRemark() { return remark; }
  /**
   * 备注。
   */
  /**
   * 设置备注。
   *
   * @param remark 备注
   */
  public void setRemark(String remark) { this.remark = remark; }
  /**
   * 获取Checked。
   *
   * @return 是否满足对应条件
   */
  public Boolean getChecked() { return checked; }
  /**
   * checked。
   */
  /**
   * 设置Checked。
   *
   * @param checked checked
   */
  public void setChecked(Boolean checked) { this.checked = checked; }
}
