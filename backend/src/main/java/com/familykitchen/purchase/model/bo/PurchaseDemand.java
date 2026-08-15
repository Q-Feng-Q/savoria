package com.familykitchen.purchase.model.bo;

import com.familykitchen.purchase.model.enums.OrderSourceStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 表示采购Demand领域计算过程中的业务数据。
 *
 * @param familyId 家庭标识
 * @param mealSlotId mealSlot标识
 * @param orderId 订单标识
 * @param dishId 菜品标识
 * @param serviceDate serviceDate
 * @param orderStatus 订单状态
 * @param ingredients ingredients
 */
public record PurchaseDemand(
    Long familyId,
    Long mealSlotId,
    Long orderId,
    Long dishId,
    LocalDate serviceDate,
    OrderSourceStatus orderStatus,
    List<IngredientDemand> ingredients
) {
}


