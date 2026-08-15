package com.familykitchen.admin.model.entity;

/** 映射平台管理端维护的商户基本资料。 */
public class AdminMerchantDO {
  /** 商户标识。 */
  private Long id;
  /** 商户名称。 */
  private String name,
      /** 启用状态。 */
      status,
      /** 联系人姓名。 */
      contactName,
      /** 联系电话。 */
      contactPhone;

  /**
   * 获取商户标识。
   *
   * @return 商户标识
   */
  public Long getId() { return id; }

  /**
   * 设置商户标识。
   *
   * @param v 商户标识
   */
  public void setId(Long v) { id = v; }

  /**
   * 获取商户名称。
   *
   * @return 商户名称
   */
  public String getName() { return name; }

  /**
   * 设置商户名称。
   *
   * @param v 商户名称
   */
  public void setName(String v) { name = v; }

  /**
   * 获取商户状态。
   *
   * @return 商户状态
   */
  public String getStatus() { return status; }

  /**
   * 设置商户状态。
   *
   * @param v 商户状态
   */
  public void setStatus(String v) { status = v; }

  /**
   * 获取联系人姓名。
   *
   * @return 联系人姓名
   */
  public String getContactName() { return contactName; }

  /**
   * 设置联系人姓名。
   *
   * @param v 联系人姓名
   */
  public void setContactName(String v) { contactName = v; }

  /**
   * 获取联系电话。
   *
   * @return 联系电话
   */
  public String getContactPhone() { return contactPhone; }

  /**
   * 设置联系电话。
   *
   * @param v 联系电话
   */
  public void setContactPhone(String v) { contactPhone = v; }
}
