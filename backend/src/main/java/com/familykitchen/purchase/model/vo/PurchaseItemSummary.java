package com.familykitchen.purchase.model.vo;

import com.familykitchen.purchase.model.enums.PurchaseSourceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 封装返回给调用方的采购项目Summary数据。
 *
 * @param ingredientName 食材名称
 * @param quantity quantity
 * @param unit unit
 * @param sourceStatus 来源状态
 * @param sources sources
 */
public record PurchaseItemSummary(
    String ingredientName,
    BigDecimal quantity,
    String unit,
    PurchaseSourceStatus sourceStatus,
    List<PurchaseSource> sources
) {

  /**
   * 封装返回给调用方的采购来源数据。
   *
   * @param familyId 家庭标识
   * @param mealSlotId mealSlot标识
   * @param orderId 订单标识
   * @param dishId 菜品标识
   * @param serviceDate serviceDate
   */
  public record PurchaseSource(
      Long familyId,
      Long mealSlotId,
      Long orderId,
      Long dishId,
      LocalDate serviceDate
  ) {
  }
}

