package com.familykitchen.family.model.vo;

import com.familykitchen.family.model.vo.AddressView;
import java.math.BigDecimal;
import java.util.List;

/**
 * 封装返回给调用方的商户家庭详情数据。
 *
 * @param familyId 家庭标识
 * @param familyName 家庭名称
 * @param merchantId 商户标识
 * @param merchantName 商户名称
 * @param note note
 * @param contactNames 联系人Names
 * @param deliveryEnabled 配送是否启用
 * @param deliveryFeeDefault 配送费用Default
 * @param deliveryFree 配送Free
 * @param deliverySummary 配送Summary
 * @param addresses addresses
 * @param members members
 * @param activeMenuCount active菜单数量
 */
public record MerchantFamilyDetailView(
    Long familyId,
    String familyName,
    Long merchantId,
    String merchantName,
    String note,
    List<String> contactNames,
    boolean deliveryEnabled,
    BigDecimal deliveryFeeDefault,
    boolean deliveryFree,
    String deliverySummary,
    List<AddressView> addresses,
    List<MemberView> members,
    int activeMenuCount
) {

  /**
   * 封装返回给调用方的成员数据。
   *
   * @param memberId 成员标识
   * @param name 名称
   * @param availableBalance available余额
   * @param frozenBalance frozen余额
   * @param lowBalance low余额
   */
  public record MemberView(
      Long memberId,
      String name,
      BigDecimal availableBalance,
      BigDecimal frozenBalance,
      boolean lowBalance
  ) {
  }
}


