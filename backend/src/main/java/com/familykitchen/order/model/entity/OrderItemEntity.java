package com.familykitchen.order.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import org.apache.ibatis.type.Alias;

/**
 * 订单菜品明细持久化实体。
 *
 * <p>对应 `order_items` 表，记录订单中的每一道菜品快照。</p>
 */
@Alias("orderItemDO")
@TableName("order_items")
public class OrderItemEntity {

  /** 所属订单 ID。 */
  @TableField("order_id")
  private Long orderId;

  /** 菜品 ID。 */
  @TableField("dish_id")
  private Long dishId;

  /** 菜品归属成员 ID。 */
  @TableField("owner_user_id")
  private Long ownerMemberId;

  /** 菜品名称快照。 */
  @TableField("dish_name_snapshot")
  private String dishNameSnapshot;

  /** 下单时单价。 */
  @TableField("price")
  private BigDecimal price;

  /** 数量。 */
  @TableField("quantity")
  private Integer quantity;

  /** 行项目金额。 */
  @TableField("amount")
  private BigDecimal amount;

  /** 单项备注。 */
  @TableField("item_remark")
  private String itemRemark;

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
   * 获取负责人成员标识。
   *
   * @return 获取负责人成员标识的结果
   */
  public Long getOwnerMemberId() { return ownerMemberId; }
  /**
   * 负责人成员标识。
   */
  /**
   * 设置负责人成员标识。
   *
   * @param ownerMemberId 负责人成员标识
   */
  public void setOwnerMemberId(Long ownerMemberId) { this.ownerMemberId = ownerMemberId; }
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
   * 获取金额。
   *
   * @return 获取金额的结果
   */
  public BigDecimal getAmount() { return amount; }
  /**
   * 金额。
   */
  /**
   * 设置金额。
   *
   * @param amount 金额
   */
  public void setAmount(BigDecimal amount) { this.amount = amount; }
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
