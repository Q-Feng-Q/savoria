package com.familykitchen.order.model.vo;

import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 封装返回给调用方的订单数据。
 *
 * @param orderId 订单标识
 * @param sourceCartId source cart identifier
 * @param merchantId 商户标识
 * @param familyId 家庭标识
 * @param submitterMemberId submitter成员标识
 * @param mealSlotId mealSlot标识
 * @param serviceDate serviceDate
 * @param expectedMealTime expected meal time
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
    Long sourceCartId,
    Long merchantId,
    Long familyId,
    Long submitterMemberId,
    Long mealSlotId,
    LocalDate serviceDate,
    LocalDateTime expectedMealTime,
    DeliveryMode deliveryMode,
    BigDecimal deliveryFee,
    OrderStatus status,
    BigDecimal totalAmount,
    String remark,
    String cancelReason,
    List<OrderItemView> items
) {
  /** Compatibility constructor for existing historical view assembly.
   * @param orderId order identifier
   * @param merchantId merchant identifier
   * @param familyId family identifier
   * @param submitterMemberId submitter identifier
   * @param mealSlotId historical meal-slot identifier
   * @param serviceDate historical service date
   * @param deliveryMode delivery mode
   * @param deliveryFee delivery fee
   * @param status order status
   * @param totalAmount total amount
   * @param remark order remark
   * @param cancelReason cancellation reason
   * @param items order items
   */
  public OrderView(Long orderId,Long merchantId,Long familyId,Long submitterMemberId,
      Long mealSlotId,LocalDate serviceDate,DeliveryMode deliveryMode,BigDecimal deliveryFee,
      OrderStatus status,BigDecimal totalAmount,String remark,String cancelReason,
      List<OrderItemView> items) {
    this(orderId,null,merchantId,familyId,submitterMemberId,mealSlotId,serviceDate,null,
        deliveryMode,deliveryFee,status,totalAmount,remark,cancelReason,items);
  }

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
   * @param selections member attribution details
   */
  public record OrderItemView(
      Long dishId,
      String dishName,
      Long ownerMemberId,
      BigDecimal price,
      int quantity,
      BigDecimal amount,
      String itemRemark
      ,List<MemberSelectionView> selections
  ) {
    /** Compatibility constructor for a historical owner row.
     * @param dishId dish identifier
     * @param dishName dish name
     * @param ownerMemberId legacy owner identifier
     * @param price price snapshot
     * @param quantity quantity
     * @param amount amount
     * @param itemRemark item remark
     */
    public OrderItemView(Long dishId,String dishName,Long ownerMemberId,BigDecimal price,
        int quantity,BigDecimal amount,String itemRemark) {
      this(dishId,dishName,ownerMemberId,price,quantity,amount,itemRemark,List.of());
    }
  }
  /** Member attribution visible in order details.
   * @param userId member identifier
   * @param memberName immutable name snapshot
   * @param quantity selected quantity
   * @param itemRemark selection remark
   */
  public record MemberSelectionView(Long userId,String memberName,int quantity,String itemRemark) {}
}

