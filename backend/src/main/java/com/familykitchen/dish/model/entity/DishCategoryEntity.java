package com.familykitchen.dish.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 菜品分类持久化实体。
 */
@TableName("dish_categories")
public class DishCategoryEntity {

  /** 分类 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 商户 ID。 */
  @TableField("merchant_id")
  private Long merchantId;

  /** 分类名称。 */
  @TableField("name")
  private String name;

  /** 排序值。 */
  @TableField("sort_order")
  private Integer sortOrder;

  /** 是否启用。 */
  @TableField("enabled")
  private Boolean enabled;

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
   * 获取Sort订单。
   *
   * @return 获取Sort订单的结果
   */
  public Integer getSortOrder() { return sortOrder; }
  /**
   * sort订单。
   */
  /**
   * 设置Sort订单。
   *
   * @param sortOrder sort订单
   */
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  /**
   * 获取是否启用。
   *
   * @return 是否满足对应条件
   */
  public Boolean getEnabled() { return enabled; }
  /**
   * 是否启用。
   */
  /**
   * 设置是否启用。
   *
   * @param enabled 是否启用
   */
  public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
