package com.familykitchen.cart.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

/**
 * 餐篮持久化实体。
 *
 * <p>对应 `carts` 表，用于保存某个家庭成员在指定日期和餐次下的点餐篮。</p>
 */
@TableName("carts")
public class CartEntity {

  /** 餐篮 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 商户 ID。 */
  @TableField("merchant_id")
  private Long merchantId;

  /** 家庭 ID。 */
  @TableField("family_id")
  private Long familyId;

  /** 成员 ID。 */
  @TableField("user_id")
  private Long memberId;

  /** 餐次 ID。 */
  @TableField("meal_slot_id")
  private Long mealSlotId;

  /** 服务日期。 */
  @TableField("service_date")
  private LocalDate serviceDate;

  /** 餐篮备注。 */
  @TableField("remark")
  private String remark;

  /** 餐篮状态。 */
  @TableField("status")
  private String status;

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
   * 获取成员标识。
   *
   * @return 获取成员标识的结果
   */
  public Long getMemberId() { return memberId; }
  /**
   * 成员标识。
   */
  /**
   * 设置成员标识。
   *
   * @param memberId 成员标识
   */
  public void setMemberId(Long memberId) { this.memberId = memberId; }
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
}
