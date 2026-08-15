package com.familykitchen.purchase.model.vo;

import com.familykitchen.purchase.model.enums.PurchaseSourceStatus;
import java.math.BigDecimal;

/**
 * 封装返回给调用方的Checkout采购项目数据。
 *
 * @param itemId 项目标识
 * @param mealSlotId 餐次标识，可为空
 * @param ingredientName 食材名称
 * @param quantity quantity
 * @param unit unit
 * @param sourceStatus 来源状态
 * @param remark 备注
 * @param familyId 家庭标识
 * @param checked checked
 * @param temporary temporary
 */
public record CheckoutPurchaseItemView(
    Long itemId,
    Long mealSlotId,
    String ingredientName,
    BigDecimal quantity,
    String unit,
    PurchaseSourceStatus sourceStatus,
    String remark,
    Long familyId,
    boolean checked,
    boolean temporary
) {
}

