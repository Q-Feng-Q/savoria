package com.familykitchen.cart.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 封装返回给调用方的购物车数据。
 *
 * @param familyId 家庭标识
 * @param mealSlotId mealSlot标识
 * @param date date
 * @param remark 备注
 * @param totalQuantity totalQuantity
 * @param totalAmount total金额
 * @param items 项目列表
 */
public record CartView(
    Long familyId,
    Long mealSlotId,
    LocalDate date,
    String remark,
    int totalQuantity,
    BigDecimal totalAmount,
    List<CartItemView> items
) {

  /**
   * 封装返回给调用方的购物车项目数据。
   *
   * @param itemId 项目标识
   * @param dishId 菜品标识
   * @param dishName 菜品名称
   * @param price price
   * @param quantity quantity
   * @param ownerMemberId 负责人成员标识
   * @param ownerMemberName 负责人成员名称
   * @param editable editable
   * @param itemRemark 项目备注
   */
  public record CartItemView(
      Long itemId,
      Long dishId,
      String dishName,
      BigDecimal price,
      int quantity,
      Long ownerMemberId,
      String ownerMemberName,
      boolean editable,
      String itemRemark
  ) {
  }
}

