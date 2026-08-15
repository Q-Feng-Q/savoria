package com.familykitchen.admin.model.vo;

/**
 * 封装返回给调用方的平台管理家庭选项数据。
 *
 * @param familyId 家庭标识
 * @param familyName 家庭名称
 * @param merchantId 商户标识
 * @param merchantName 商户名称
 * @param status 状态
 * @param memberCount 成员数量
 */
public record AdminFamilyOptionView(
    Long familyId,
    String familyName,
    Long merchantId,
    String merchantName,
    String status,
    Integer memberCount
) {
}
