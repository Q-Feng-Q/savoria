package com.familykitchen.admin.model.vo;

import com.familykitchen.family.model.vo.AddressView;
import java.math.BigDecimal;
import java.util.List;

/**
 * 封装返回给调用方的平台管理家庭详情数据。
 *
 * @param familyId 家庭标识
 * @param familyName 家庭名称
 * @param merchantId 商户标识
 * @param merchantName 商户名称
 * @param status 状态
 * @param note note
 * @param deliveryEnabled 配送是否启用
 * @param deliveryFeeDefault 配送费用Default
 * @param deliveryFree 配送Free
 * @param members members
 * @param addresses addresses
 */
public record AdminFamilyDetailView(
    Long familyId,
    String familyName,
    Long merchantId,
    String merchantName,
    String status,
    String note,
    Boolean deliveryEnabled,
    BigDecimal deliveryFeeDefault,
    Boolean deliveryFree,
    List<AdminFamilyMemberView> members,
    List<AddressView> addresses
) {
}
