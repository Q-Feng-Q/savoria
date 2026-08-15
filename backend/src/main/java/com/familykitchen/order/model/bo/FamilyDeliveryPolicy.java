package com.familykitchen.order.model.bo;

import java.math.BigDecimal;

/**
 * 表示家庭配送Policy领域计算过程中的业务数据。
 *
 * @param deliveryEnabled 配送是否启用
 * @param deliveryFeeDefault 配送费用Default
 * @param deliveryFeeFree 配送费用Free
 */
public record FamilyDeliveryPolicy(
        boolean deliveryEnabled,
        BigDecimal deliveryFeeDefault,
        boolean deliveryFeeFree
) {
}
