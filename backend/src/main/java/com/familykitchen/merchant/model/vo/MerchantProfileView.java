package com.familykitchen.merchant.model.vo;

/**
 * 商户负责人可查看和编辑的商户业务资料。
 *
 * @param name 商户名称
 * @param contactName 联系人姓名
 * @param contactPhone 联系电话
 */
public record MerchantProfileView(String name, String contactName, String contactPhone) {
}
