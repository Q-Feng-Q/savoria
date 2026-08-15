package com.familykitchen.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.ibatis.type.Alias;

/**
 * 订单主表持久化实体。
 *
 * <p>对应 `orders` 表，保存订单头信息，不包含菜品明细和配送快照。</p>
 */
@Alias("orderRecordDO")
@TableName("orders")
public class OrderRecordEntity {

  /** 订单 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 商户 ID。 */
  @TableField("merchant_id")
  private Long merchantId;

  /** 家庭 ID。 */
  @TableField("family_id")
  private Long familyId;

  /** 提交订单的成员 ID。 */
  @TableField("submitter_user_id")
  private Long submitterMemberId;

  /** 餐次 ID。 */
  @TableField("meal_slot_id")
  private Long mealSlotId;

  /** 服务日期。 */
  @TableField("service_date")
  private LocalDate serviceDate;

  /** 配送方式编码。 */
  @TableField("delivery_mode")
  private String deliveryMode;

  /** 配送费。 */
  @TableField("delivery_fee")
  private BigDecimal deliveryFee;

  /** 配送费承担成员 ID。 */
  @TableField("delivery_fee_payer_user_id")
  private Long deliveryFeePayerMemberId;

  /** 订单状态编码。 */
  @TableField("status")
  private String status;

  /** 订单总金额。 */
  @TableField("total_amount")
  private BigDecimal totalAmount;

  /** 下单备注。 */
  @TableField("remark")
  private String remark;

  /** 取消原因。 */
  @TableField("cancel_reason")
  private String cancelReason;

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
   * 获取Submitter成员标识。
   *
   * @return 获取Submitter成员标识的结果
   */
  public Long getSubmitterMemberId() { return submitterMemberId; }
  /**
   * submitter成员标识。
   */
  /**
   * 设置Submitter成员标识。
   *
   * @param submitterMemberId submitter成员标识
   */
  public void setSubmitterMemberId(Long submitterMemberId) { this.submitterMemberId = submitterMemberId; }
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
   * 获取配送Mode。
   *
   * @return 获取配送Mode的结果
   */
  public String getDeliveryMode() { return deliveryMode; }
  /**
   * 配送Mode。
   */
  /**
   * 设置配送Mode。
   *
   * @param deliveryMode 配送Mode
   */
  public void setDeliveryMode(String deliveryMode) { this.deliveryMode = deliveryMode; }
  /**
   * 获取配送费用。
   *
   * @return 获取配送费用的结果
   */
  public BigDecimal getDeliveryFee() { return deliveryFee; }
  /**
   * 配送费用。
   */
  /**
   * 设置配送费用。
   *
   * @param deliveryFee 配送费用
   */
  public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
  /**
   * 获取配送费用Payer成员标识。
   *
   * @return 获取配送费用Payer成员标识的结果
   */
  public Long getDeliveryFeePayerMemberId() { return deliveryFeePayerMemberId; }
  /**
   * 配送费用Payer成员标识。
   */
  /**
   * 设置配送费用Payer成员标识。
   *
   * @param deliveryFeePayerMemberId 配送费用Payer成员标识
   */
  public void setDeliveryFeePayerMemberId(Long deliveryFeePayerMemberId) { this.deliveryFeePayerMemberId = deliveryFeePayerMemberId; }
  /**
   * 获取状态。
   *
   * @return 获取状态的结果
   */
  public String getStatus() { return status; }
  /**
   * 状态。
   */
  /**
   * 设置状态。
   *
   * @param status 状态
   */
  public void setStatus(String status) { this.status = status; }
  /**
   * 获取Total金额。
   *
   * @return 获取Total金额的结果
   */
  public BigDecimal getTotalAmount() { return totalAmount; }
  /**
   * total金额。
   */
  /**
   * 设置Total金额。
   *
   * @param totalAmount total金额
   */
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
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
   * 获取Cancel原因。
   *
   * @return 获取Cancel原因的结果
   */
  public String getCancelReason() { return cancelReason; }
  /**
   * cancel原因。
   */
  /**
   * 设置Cancel原因。
   *
   * @param cancelReason cancel原因
   */
  public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
