package com.familykitchen.family.model.vo;

import java.math.BigDecimal;

/**
 * 当前家庭可由家庭端查看的业务资料。
 *
 * @param familyName 家庭名称
 * @param note 家庭备注
 * @param merchantName 所属商户名称
 * @param deliveryEnabled 是否支持配送
 * @param deliveryFeeDefault 默认配送费
 * @param deliveryFree 是否免配送费
 */
public record FamilyInfoView(
    String familyName,
    String note,
    String merchantName,
    boolean deliveryEnabled,
    BigDecimal deliveryFeeDefault,
    boolean deliveryFree
) {
}
