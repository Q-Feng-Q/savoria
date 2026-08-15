package com.familykitchen.cart.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

/**
 * 餐篮菜品持久化实体。
 *
 * <p>对应 `cart_items` 表，保存菜品快照、数量和单品备注。</p>
 */
@TableName("cart_items")
public class CartItemEntity {

  /** 餐篮项 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 所属餐篮 ID。 */
  @TableField("cart_id")
  private Long cartId;

  /** 菜品 ID。 */
  @TableField("dish_id")
  private Long dishId;

  /** 菜品名称快照。 */
  @TableField("dish_name_snapshot")
  private String dishNameSnapshot;

  /** 下单单价快照。 */
  @TableField("price")
  private BigDecimal price;

  /** 份数。 */
  @TableField("quantity")
  private Integer quantity;

  /** 单品备注。 */
  @TableField("item_remark")
  private String itemRemark;

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
   * 获取购物车标识。
   *
   * @return 获取购物车标识的结果
   */
  public Long getCartId() { return cartId; }
  /**
   * 购物车标识。
   */
  /**
   * 设置购物车标识。
   *
   * @param cartId 购物车标识
   */
  public void setCartId(Long cartId) { this.cartId = cartId; }
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
   * 获取菜品名称Snapshot。
   *
   * @return 获取菜品名称Snapshot的结果
   */
  public String getDishNameSnapshot() { return dishNameSnapshot; }
  /**
   * 菜品名称Snapshot。
   */
  /**
   * 设置菜品名称Snapshot。
   *
   * @param dishNameSnapshot 菜品名称Snapshot
   */
  public void setDishNameSnapshot(String dishNameSnapshot) { this.dishNameSnapshot = dishNameSnapshot; }
  /**
   * 获取Price。
   *
   * @return 获取Price的结果
   */
  public BigDecimal getPrice() { return price; }
  /**
   * price。
   */
  /**
   * 设置Price。
   *
   * @param price price
   */
  public void setPrice(BigDecimal price) { this.price = price; }
  /**
   * 获取Quantity。
   *
   * @return 获取Quantity的结果
   */
  public Integer getQuantity() { return quantity; }
  /**
   * quantity。
   */
  /**
   * 设置Quantity。
   *
   * @param quantity quantity
   */
  public void setQuantity(Integer quantity) { this.quantity = quantity; }
  /**
   * 获取项目备注。
   *
   * @return 获取项目备注的结果
   */
  public String getItemRemark() { return itemRemark; }
  /**
   * 项目备注。
   */
  /**
   * 设置项目备注。
   *
   * @param itemRemark 项目备注
   */
  public void setItemRemark(String itemRemark) { this.itemRemark = itemRemark; }
}
