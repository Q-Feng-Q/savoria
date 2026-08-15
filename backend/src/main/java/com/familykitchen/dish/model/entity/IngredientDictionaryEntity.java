package com.familykitchen.dish.model.entity;

/**
 * 商户食材字典持久化实体。
 */
public class IngredientDictionaryEntity {

  /**
   * 标识。
   */
  private Long id;
  /**
   * 商户标识。
   */
  private Long merchantId;
  /**
   * 名称。
   */
  private String name;
  /**
   * category。
   */
  private String category;
  /**
   * unit。
   */
  private String unit;
  /**
   * referenced菜品数量。
   */
  private Integer referencedDishCount;
  /**
   * referenced菜品Names。
   */
  private String referencedDishNames;

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
   * 获取Category。
   *
   * @return 获取Category的结果
   */
  public String getCategory() { return category; }
  /**
   * category。
   */
  /**
   * 设置Category。
   *
   * @param category category
   */
  public void setCategory(String category) { this.category = category; }
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
   * 获取Referenced菜品数量。
   *
   * @return 获取Referenced菜品数量的结果
   */
  public Integer getReferencedDishCount() { return referencedDishCount; }
  /**
   * referenced菜品数量。
   */
  /**
   * 设置Referenced菜品数量。
   *
   * @param referencedDishCount referenced菜品数量
   */
  public void setReferencedDishCount(Integer referencedDishCount) { this.referencedDishCount = referencedDishCount; }
  /**
   * 获取Referenced菜品Names。
   *
   * @return 获取Referenced菜品Names的结果
   */
  public String getReferencedDishNames() { return referencedDishNames; }
  /**
   * referenced菜品Names。
   */
  /**
   * 设置Referenced菜品Names。
   *
   * @param referencedDishNames referenced菜品Names
   */
  public void setReferencedDishNames(String referencedDishNames) { this.referencedDishNames = referencedDishNames; }
}
