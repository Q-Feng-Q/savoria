package com.familykitchen.family.model.entity;

import java.math.BigDecimal;

/**
 * 家庭资料查询行对象。
 */
public class FamilyRecord {

  /**
   * 家庭标识。
   */
  private Long familyId;
  /**
   * 家庭名称。
   */
  private String familyName;
  /**
   * 商户标识。
   */
  private Long merchantId;
  /**
   * 商户名称。
   */
  private String merchantName;
  /**
   * note。
   */
  private String note;
  /**
   * 联系人NamesJson。
   */
  private String contactNamesJson;
  /**
   * 配送是否启用。
   */
  private Boolean deliveryEnabled;
  /**
   * 配送费用Default。
   */
  private BigDecimal deliveryFeeDefault;
  /**
   * 配送Free。
   */
  private Boolean deliveryFree;
  /**
   * default地址Text。
   */
  private String defaultAddressText;
  /**
   * 地址数量。
   */
  private Integer addressCount;
  /**
   * 成员数量。
   */
  private Integer memberCount;
  /**
   * active菜单数量。
   */
  private Integer activeMenuCount;
  /**
   * low余额成员数量。
   */
  private Integer lowBalanceMemberCount;
  /**
   * frozen余额Total。
   */
  private BigDecimal frozenBalanceTotal;

  /**
   * 获取家庭标识。
   *
   * @return 获取家庭标识的结果
   */
  public Long getFamilyId() { return familyId; }
  /**
   * 家庭标识。
   */
  /**
   * 设置家庭标识。
   *
   * @param familyId 家庭标识
   */
  public void setFamilyId(Long familyId) { this.familyId = familyId; }
  /**
   * 获取家庭名称。
   *
   * @return 获取家庭名称的结果
   */
  public String getFamilyName() { return familyName; }
  /**
   * 家庭名称。
   */
  /**
   * 设置家庭名称。
   *
   * @param familyName 家庭名称
   */
  public void setFamilyName(String familyName) { this.familyName = familyName; }
  /**
   * 获取商户标识。
   *
   * @return 获取商户标识的结果
   */
  public Long getMerchantId() { return merchantId; }
  /**
   * 商户标识。
   */
  /**
   * 设置商户标识。
   *
   * @param merchantId 商户标识
   */
  public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
  /**
   * 获取商户名称。
   *
   * @return 获取商户名称的结果
   */
  public String getMerchantName() { return merchantName; }
  /**
   * 商户名称。
   */
  /**
   * 设置商户名称。
   *
   * @param merchantName 商户名称
   */
  public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
  /**
   * 获取Note。
   *
   * @return 获取Note的结果
   */
  public String getNote() { return note; }
  /**
   * note。
   */
  /**
   * 设置Note。
   *
   * @param note note
   */
  public void setNote(String note) { this.note = note; }
  /**
   * 获取联系人NamesJson。
   *
   * @return 获取联系人NamesJson的结果
   */
  public String getContactNamesJson() { return contactNamesJson; }
  /**
   * 联系人NamesJson。
   */
  /**
   * 设置联系人NamesJson。
   *
   * @param contactNamesJson 联系人NamesJson
   */
  public void setContactNamesJson(String contactNamesJson) { this.contactNamesJson = contactNamesJson; }
  /**
   * 获取配送是否启用。
   *
   * @return 是否满足对应条件
   */
  public Boolean getDeliveryEnabled() { return deliveryEnabled; }
  /**
   * 配送是否启用。
   */
  /**
   * 设置配送是否启用。
   *
   * @param deliveryEnabled 配送是否启用
   */
  public void setDeliveryEnabled(Boolean deliveryEnabled) { this.deliveryEnabled = deliveryEnabled; }
  /**
   * 获取配送费用Default。
   *
   * @return 获取配送费用Default的结果
   */
  public BigDecimal getDeliveryFeeDefault() { return deliveryFeeDefault; }
  /**
   * 配送费用Default。
   */
  /**
   * 设置配送费用Default。
   *
   * @param deliveryFeeDefault 配送费用Default
   */
  public void setDeliveryFeeDefault(BigDecimal deliveryFeeDefault) { this.deliveryFeeDefault = deliveryFeeDefault; }
  /**
   * 获取配送Free。
   *
   * @return 是否满足对应条件
   */
  public Boolean getDeliveryFree() { return deliveryFree; }
  /**
   * 配送Free。
   */
  /**
   * 设置配送Free。
   *
   * @param deliveryFree 配送Free
   */
  public void setDeliveryFree(Boolean deliveryFree) { this.deliveryFree = deliveryFree; }
  /**
   * 获取Default地址Text。
   *
   * @return 获取Default地址Text的结果
   */
  public String getDefaultAddressText() { return defaultAddressText; }
  /**
   * default地址Text。
   */
  /**
   * 设置Default地址Text。
   *
   * @param defaultAddressText default地址Text
   */
  public void setDefaultAddressText(String defaultAddressText) { this.defaultAddressText = defaultAddressText; }
  /**
   * 获取地址数量。
   *
   * @return 获取地址数量的结果
   */
  public Integer getAddressCount() { return addressCount; }
  /**
   * 地址数量。
   */
  /**
   * 设置地址数量。
   *
   * @param addressCount 地址数量
   */
  public void setAddressCount(Integer addressCount) { this.addressCount = addressCount; }
  /**
   * 获取成员数量。
   *
   * @return 获取成员数量的结果
   */
  public Integer getMemberCount() { return memberCount; }
  /**
   * 成员数量。
   */
  /**
   * 设置成员数量。
   *
   * @param memberCount 成员数量
   */
  public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
  /**
   * 获取Active菜单数量。
   *
   * @return 获取Active菜单数量的结果
   */
  public Integer getActiveMenuCount() { return activeMenuCount; }
  /**
   * active菜单数量。
   */
  /**
   * 设置Active菜单数量。
   *
   * @param activeMenuCount active菜单数量
   */
  public void setActiveMenuCount(Integer activeMenuCount) { this.activeMenuCount = activeMenuCount; }
  /**
   * 获取Low余额成员数量。
   *
   * @return 获取Low余额成员数量的结果
   */
  public Integer getLowBalanceMemberCount() { return lowBalanceMemberCount; }
  /**
   * low余额成员数量。
   */
  /**
   * 设置Low余额成员数量。
   *
   * @param lowBalanceMemberCount low余额成员数量
   */
  public void setLowBalanceMemberCount(Integer lowBalanceMemberCount) { this.lowBalanceMemberCount = lowBalanceMemberCount; }
  /**
   * 获取Frozen余额Total。
   *
   * @return 获取Frozen余额Total的结果
   */
  public BigDecimal getFrozenBalanceTotal() { return frozenBalanceTotal; }
  /**
   * frozen余额Total。
   */
  /**
   * 设置Frozen余额Total。
   *
   * @param frozenBalanceTotal frozen余额Total
   */
  public void setFrozenBalanceTotal(BigDecimal frozenBalanceTotal) { this.frozenBalanceTotal = frozenBalanceTotal; }
}
