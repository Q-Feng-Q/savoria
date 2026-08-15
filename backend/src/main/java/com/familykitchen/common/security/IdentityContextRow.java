package com.familykitchen.common.security;

/** 数据库查询返回的实时用户身份摘要，用于组装请求级权限上下文。 */
public class IdentityContextRow {
  private Long userId;
  private Long familyId;
  private Long merchantId;
  private String familyRole;
  /** 返回账号标识。
   * @return 账号标识 */
  public Long getUserId() { return userId; }
  /** 设置账号标识。
   * @param userId 账号标识 */
  public void setUserId(Long userId) { this.userId = userId; }
  /** 返回当前家庭标识。
   * @return 当前家庭标识 */
  public Long getFamilyId() { return familyId; }
  /** 设置当前家庭标识。
   * @param familyId 当前家庭标识 */
  public void setFamilyId(Long familyId) { this.familyId = familyId; }
  /** 返回关联商户标识。
   * @return 关联商户标识 */
  public Long getMerchantId() { return merchantId; }
  /** 设置关联商户标识。
   * @param merchantId 关联商户标识 */
  public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
  /** 返回家庭角色编码。
   * @return 当前用户的家庭角色编码 */
  public String getFamilyRole() { return familyRole; }
  /** 设置家庭角色编码。
   * @param familyRole 当前用户的家庭角色编码 */
  public void setFamilyRole(String familyRole) { this.familyRole = familyRole; }
}
