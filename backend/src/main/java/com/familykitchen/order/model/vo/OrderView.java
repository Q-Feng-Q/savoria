package com.familykitchen.order.model.vo;

import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 封装返回给调用方的订单数据。
 *
 * @param orderId 订单标识
 * @param merchantId 商户标识
 * @param familyId 家庭标识
 * @param submitterMemberId submitter成员标识
 * @param mealSlotId mealSlot标识
 * @param serviceDate serviceDate
 * @param deliveryMode 配送Mode
 * @param deliveryFee 配送费用
 * @param status 状态
 * @param totalAmount total金额
 * @param remark 备注
 * @param cancelReason cancel原因
 * @param items 项目列表
 */
public record OrderView(
    Long orderId,
    Long merchantId,
    Long familyId,
    Long submitterMemberId,
    Long mealSlotId,
    LocalDate serviceDate,
    DeliveryMode deliveryMode,
    BigDecimal deliveryFee,
    OrderStatus status,
    BigDecimal totalAmount,
    String remark,
    String cancelReason,
    List<OrderItemView> items
) {

  /**
   * 封装返回给调用方的订单项目数据。
   *
   * @param dishId 菜品标识
   * @param dishName 菜品名称
   * @param ownerMemberId 负责人成员标识
   * @param price price
   * @param quantity quantity
   * @param amount 金额
   * @param itemRemark 项目备注
   */
  public record OrderItemView(
      Long dishId,
      String dishName,
      Long ownerMemberId,
      BigDecimal price,
      int quantity,
      BigDecimal amount,
      String itemRemark
  ) {
  }
}

