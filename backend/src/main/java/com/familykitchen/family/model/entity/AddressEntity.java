package com.familykitchen.family.model.entity;

/**
 * 家庭地址持久化实体。
 */
public class AddressEntity {
  /**
   * 标识。
   */
  private Long id;
  /**
   * 家庭标识。
   */
  private Long familyId;
  /**
   * 联系人名称。
   */
  private String contactName;
  /**
   * 联系人联系电话。
   */
  private String contactPhone;
  /**
   * 地址Text。
   */
  private String addressText;
  /**
   * default地址。
   */
  private Boolean defaultAddress;

  /**
   * 获取标识。
   *
   * @return 获取标识的结果
   */
  public Long getId() { return id; }
  /**
   * 标识。
   */
  /**
   * 设置标识。
   *
   * @param id 标识
   */
  public void setId(Long id) { this.id = id; }
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
   * 获取联系人名称。
   *
   * @return 获取联系人名称的结果
   */
  public String getContactName() { return contactName; }
  /**
   * 联系人名称。
   */
  /**
   * 设置联系人名称。
   *
   * @param contactName 联系人名称
   */
  public void setContactName(String contactName) { this.contactName = contactName; }
  /**
   * 获取联系人联系电话。
   *
   * @return 获取联系人联系电话的结果
   */
  public String getContactPhone() { return contactPhone; }
  /**
   * 联系人联系电话。
   */
  /**
   * 设置联系人联系电话。
   *
   * @param contactPhone 联系人联系电话
   */
  public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
  /**
   * 获取地址Text。
   *
   * @return 获取地址Text的结果
   */
  public String getAddressText() { return addressText; }
  /**
   * 地址Text。
   */
  /**
   * 设置地址Text。
   *
   * @param addressText 地址Text
   */
  public void setAddressText(String addressText) { this.addressText = addressText; }
  /**
   * 获取Default地址。
   *
   * @return 是否满足对应条件
   */
  public Boolean getDefaultAddress() { return defaultAddress; }
  /**
   * default地址。
   */
  /**
   * 设置Default地址。
   *
   * @param defaultAddress default地址
   */
  public void setDefaultAddress(Boolean defaultAddress) { this.defaultAddress = defaultAddress; }
}
