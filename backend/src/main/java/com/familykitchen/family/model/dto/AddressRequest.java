package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 承载地址相关的请求参数。
 *
 * @param contactName 联系人名称
 * @param contactPhone 联系人联系电话
 * @param addressText 地址Text
 * @param defaultAddress default地址
 */
public record AddressRequest(
    @NotBlank String contactName,
    @NotBlank String contactPhone,
    @NotBlank String addressText,
    boolean defaultAddress
) {
}

