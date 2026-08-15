package com.familykitchen.order.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;

/**
 * 订单配送快照持久化实体。
 *
 * <p>对应 `order_delivery_snapshots` 表，用于保存下单时的联系人和配送地址快照。</p>
 */
@Alias("orderDeliverySnapshotDO")
@TableName("order_delivery_snapshots")
public class OrderDeliverySnapshotEntity {

  /** 所属订单 ID。 */
  @TableField("order_id")
  private Long orderId;

  /** 联系人姓名。 */
  @TableField("contact_name")
  private String contactName;

  /** 联系电话。 */
  @TableField("contact_phone")
  private String contactPhone;

  /** 配送地址文本。 */
  @TableField("address_text")
  private String addressText;

  /**
   * 获取订单标识。
   *
   * @return 获取订单标识的结果
   */
  public Long getOrderId() {
    return orderId;
  }

  /**
   * 订单标识。
   */
  /**
   * 设置订单标识。
   *
   * @param orderId 订单标识
   */
  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  /**
   * 获取联系人名称。
   *
   * @return 获取联系人名称的结果
   */
  public String getContactName() {
    return contactName;
  }

  /**
   * 联系人名称。
   */
  /**
   * 设置联系人名称。
   *
   * @param contactName 联系人名称
   */
  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  /**
   * 获取联系人联系电话。
   *
   * @return 获取联系人联系电话的结果
   */
  public String getContactPhone() {
    return contactPhone;
  }

  /**
   * 联系人联系电话。
   */
  /**
   * 设置联系人联系电话。
   *
   * @param contactPhone 联系人联系电话
   */
  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  /**
   * 获取地址Text。
   *
   * @return 获取地址Text的结果
   */
  public String getAddressText() {
    return addressText;
  }

  /**
   * 地址Text。
   */
  /**
   * 设置地址Text。
   *
   * @param addressText 地址Text
   */
  public void setAddressText(String addressText) {
    this.addressText = addressText;
  }
}
