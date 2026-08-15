package com.familykitchen.family.model.entity;

import java.math.BigDecimal;

/**
 * 家庭成员钱包查询行对象。
 */
public class FamilyMemberRecord {
  /**
   * 成员标识。
   */
  private Long memberId;
  /**
   * 名称。
   */
  private String name;
  /**
   * 角色Template。
   */
  private String roleTemplate;
  /**
   * available余额。
   */
  private BigDecimal availableBalance;
  /**
   * frozen余额。
   */
  private BigDecimal frozenBalance;

  /**
   * 获取成员标识。
   *
   * @return 获取成员标识的结果
   */
  public Long getMemberId() { return memberId; }
  /**
   * 成员标识。
   */
  /**
   * 设置成员标识。
   *
   * @param memberId 成员标识
   */
  public void setMemberId(Long memberId) { this.memberId = memberId; }
  /**
   * 获取名称。
   *
   * @return 获取名称的结果
   */
  public String getName() { return name; }
  /**
   * 名称。
   */
  /**
   * 设置名称。
   *
   * @param name 名称
   */
  public void setName(String name) { this.name = name; }
  /**
   * 获取角色Template。
   *
   * @return 获取角色Template的结果
   */
  public String getRoleTemplate() { return roleTemplate; }
  /**
   * 角色Template。
   */
  /**
   * 设置角色Template。
   *
   * @param roleTemplate 角色Template
   */
  public void setRoleTemplate(String roleTemplate) { this.roleTemplate = roleTemplate; }
  /**
   * 获取Available余额。
   *
   * @return 获取Available余额的结果
   */
  public BigDecimal getAvailableBalance() { return availableBalance; }
  /**
   * available余额。
   */
  /**
   * 设置Available余额。
   *
   * @param availableBalance available余额
   */
  public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
  /**
   * 获取Frozen余额。
   *
   * @return 获取Frozen余额的结果
   */
  public BigDecimal getFrozenBalance() { return frozenBalance; }
  /**
   * frozen余额。
   */
  /**
   * 设置Frozen余额。
   *
   * @param frozenBalance frozen余额
   */
  public void setFrozenBalance(BigDecimal frozenBalance) { this.frozenBalance = frozenBalance; }
}
