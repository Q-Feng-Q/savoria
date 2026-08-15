package com.familykitchen.family.model.vo;

/**
 * 封装返回给调用方的地址数据。
 *
 * @param addressId 地址标识
 * @param contactName 联系人名称
 * @param contactPhone 联系人联系电话
 * @param addressText 地址Text
 * @param defaultAddress default地址
 */
public record AddressView(
    Long addressId,
    String contactName,
    String contactPhone,
    String addressText,
    boolean defaultAddress
) {
}

