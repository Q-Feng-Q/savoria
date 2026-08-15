package com.familykitchen.order.model.bo;

import com.familykitchen.order.model.enums.DeliveryMode;

import java.time.LocalDate;
import java.util.List;

/**
 * 表示订单CheckoutCommand领域计算过程中的业务数据。
 *
 * @param merchantId 商户标识
 * @param familyId 家庭标识
 * @param submitterMemberId submitter成员标识
 * @param mealSlotId mealSlot标识
 * @param serviceDate serviceDate
 * @param deliveryMode 配送Mode
 * @param remark 备注
 * @param deliverySnapshot 配送Snapshot
 * @param familyDeliveryPolicy 家庭配送Policy
 * @param items 项目列表
 */
public record OrderCheckoutCommand(
    Long merchantId,
    Long familyId,
    Long submitterMemberId,
    Long mealSlotId,
    LocalDate serviceDate,
    DeliveryMode deliveryMode,
    String remark,
    DeliverySnapshot deliverySnapshot,
    FamilyDeliveryPolicy familyDeliveryPolicy,
    List<CheckoutItem> items
) {
}

