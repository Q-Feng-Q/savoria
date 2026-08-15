package com.familykitchen.order.model.bo;

import java.time.LocalDate;
import java.util.List;

/**
 * 表示订单购物车Snapshot领域计算过程中的业务数据。
 *
 * @param merchantId 商户标识
 * @param familyId 家庭标识
 * @param ownerMemberId 负责人成员标识
 * @param mealSlotId mealSlot标识
 * @param serviceDate serviceDate
 * @param remark 备注
 * @param items 项目列表
 */
public record OrderCartSnapshot(
    Long merchantId,
    Long familyId,
    Long ownerMemberId,
    Long mealSlotId,
    LocalDate serviceDate,
    String remark,
    List<CheckoutItem> items
) {
}
