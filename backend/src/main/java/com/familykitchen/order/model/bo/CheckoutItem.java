package com.familykitchen.order.model.bo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 表示Checkout项目领域计算过程中的业务数据。
 *
 * @param dishId 菜品标识
 * @param dishName 菜品名称
 * @param ownerMemberId 负责人成员标识
 * @param ownerMemberName 负责人成员名称
 * @param price price
 * @param quantity quantity
 * @param itemRemark 项目备注
 * @param ingredients ingredients
 */
public record CheckoutItem(
        Long dishId,
        String dishName,
        Long ownerMemberId,
        String ownerMemberName,
        BigDecimal price,
        int quantity,
        String itemRemark,
        List<CheckoutIngredient> ingredients
) {
}
