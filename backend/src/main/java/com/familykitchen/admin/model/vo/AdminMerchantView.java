package com.familykitchen.admin.model.vo;
/**
 * 封装返回给调用方的平台管理商户数据。
 *
 * @param merchantId 商户标识
 * @param name 名称
 * @param status 状态
 * @param contactName 联系人名称
 * @param contactPhone 联系人联系电话
 * @param ownerUserId 负责人用户标识
 * @param ownerUsername 负责人用户名
 * @param familyCount 家庭数量
 * @param createdAt 创建时间
 */
public record AdminMerchantView(Long merchantId,String name,String status,String contactName,String contactPhone,
    Long ownerUserId,String ownerUsername,int familyCount,String createdAt) {}
