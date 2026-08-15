package com.familykitchen.family.model.vo;

import java.math.BigDecimal;

/**
 * 封装返回给调用方的商户家庭Summary数据。
 *
 * @param familyId 家庭标识
 * @param familyName 家庭名称
 * @param merchantName 商户名称
 * @param note note
 * @param contactNames 联系人Names
 * @param deliveryEnabled 配送是否启用
 * @param deliveryFeeDefault 配送费用Default
 * @param deliveryFree 配送Free
 * @param deliverySummary 配送Summary
 * @param defaultAddressText default地址Text
 * @param addressCount 地址数量
 * @param memberCount 成员数量
 * @param activeMenuCount active菜单数量
 * @param lowBalanceMemberCount low余额成员数量
 * @param frozenBalanceTotal frozen余额Total
 */
public record MerchantFamilySummaryView(
    Long familyId,
    String familyName,
    String merchantName,
    String note,
    java.util.List<String> contactNames,
    boolean deliveryEnabled,
    BigDecimal deliveryFeeDefault,
    boolean deliveryFree,
    String deliverySummary,
    String defaultAddressText,
    int addressCount,
    int memberCount,
    int activeMenuCount,
    int lowBalanceMemberCount,
    BigDecimal frozenBalanceTotal
) {
}

