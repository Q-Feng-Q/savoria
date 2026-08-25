package com.familykitchen.merchant.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商户负责人修改自身商户业务资料的请求。
 *
 * @param name 商户名称
 * @param contactName 联系人姓名
 * @param contactPhone 联系电话
 */
public record UpdateMerchantProfileRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String contactName,
    @Size(max = 30) String contactPhone
) {
}
