package com.familykitchen.order.model.bo;

import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;

import com.familykitchen.purchase.model.bo.PurchaseDemand;
import com.familykitchen.wallet.model.bo.WalletChange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 表示订单Submission结果领域计算过程中的业务数据。
 *
 * @param order 订单
 * @param memberCharges 成员Charges
 * @param walletChanges 钱包Changes
 * @param purchaseDemands 采购Demands
 * @param notifications notifications
 */
public record OrderSubmissionResult(
    SubmittedOrder order,
    List<MemberCharge> memberCharges,
    List<WalletChange> walletChanges,
    List<PurchaseDemand> purchaseDemands,
    List<OrderNotification> notifications
) {

  /**
   * 表示Submitted订单领域计算过程中的业务数据。
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
   * @param deliverySnapshot 配送Snapshot
   * @param items 项目列表
   */
  public record SubmittedOrder(
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
      DeliverySnapshot deliverySnapshot,
      List<SubmittedOrderItem> items
  ) {
  }

  /**
   * 表示Submitted订单项目领域计算过程中的业务数据。
   *
   * @param dishId 菜品标识
   * @param dishName 菜品名称
   * @param ownerMemberId 负责人成员标识
   * @param price price
   * @param quantity quantity
   * @param amount 金额
   * @param itemRemark 项目备注
   */
  public record SubmittedOrderItem(
      Long dishId,
      String dishName,
      Long ownerMemberId,
      BigDecimal price,
      int quantity,
      BigDecimal amount,
      String itemRemark
  ) {
  }

  /**
   * 表示成员Charge领域计算过程中的业务数据。
   *
   * @param memberId 成员标识
   * @param dishAmount 菜品金额
   * @param deliveryFeeAmount 配送费用金额
   * @param totalAmount total金额
   */
  public record MemberCharge(
      Long memberId,
      BigDecimal dishAmount,
      BigDecimal deliveryFeeAmount,
      BigDecimal totalAmount
  ) {
  }

  /**
   * 表示订单通知领域计算过程中的业务数据。
   *
   * @param scope scope
   * @param receiverId receiver标识
   * @param category category
   * @param title title
   * @param content content
   */
  public record OrderNotification(
      String scope,
      Long receiverId,
      String category,
      String title,
      String content
  ) {
  }
}


