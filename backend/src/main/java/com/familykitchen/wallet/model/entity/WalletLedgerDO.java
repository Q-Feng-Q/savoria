package com.familykitchen.wallet.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.type.Alias;

/**
 * 钱包流水持久化实体。
 *
 * <p>对应 `wallet_ledgers` 表，记录每次余额和冻结金额变化前后的快照。</p>
 */
@Alias("walletLedgerDO")
@TableName("wallet_ledgers")
public class WalletLedgerDO {

  /** 流水 ID。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 成员 ID。 */
  @TableField("user_id")
  private Long memberId;

  /** 流水类型。 */
  @TableField("type")
  private String type;

  /** 变动金额。 */
  @TableField("amount")
  private BigDecimal amount;

  /** 变动前余额。 */
  @TableField("balance_before")
  private BigDecimal balanceBefore;

  /** 变动后余额。 */
  @TableField("balance_after")
  private BigDecimal balanceAfter;

  /** 变动前冻结金额。 */
  @TableField("frozen_before")
  private BigDecimal frozenBefore;

  /** 变动后冻结金额。 */
  @TableField("frozen_after")
  private BigDecimal frozenAfter;

  /** 流水备注。 */
  @TableField("remark")
  private String remark;

  /** 创建时间。 */
  @TableField("created_at")
  private LocalDateTime createdAt;

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
   * 获取类型。
   *
   * @return 获取类型的结果
   */
  public String getType() { return type; }
  /**
   * 类型。
   */
  /**
   * 设置类型。
   *
   * @param type 类型
   */
  public void setType(String type) { this.type = type; }
  /**
   * 获取金额。
   *
   * @return 获取金额的结果
   */
  public BigDecimal getAmount() { return amount; }
  /**
   * 金额。
   */
  /**
   * 设置金额。
   *
   * @param amount 金额
   */
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  /**
   * 获取余额Before。
   *
   * @return 获取余额Before的结果
   */
  public BigDecimal getBalanceBefore() { return balanceBefore; }
  /**
   * 余额Before。
   */
  /**
   * 设置余额Before。
   *
   * @param balanceBefore 余额Before
   */
  public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
  /**
   * 获取余额After。
   *
   * @return 获取余额After的结果
   */
  public BigDecimal getBalanceAfter() { return balanceAfter; }
  /**
   * 余额After。
   */
  /**
   * 设置余额After。
   *
   * @param balanceAfter 余额After
   */
  public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
  /**
   * 获取FrozenBefore。
   *
   * @return 获取FrozenBefore的结果
   */
  public BigDecimal getFrozenBefore() { return frozenBefore; }
  /**
   * frozenBefore。
   */
  /**
   * 设置FrozenBefore。
   *
   * @param frozenBefore frozenBefore
   */
  public void setFrozenBefore(BigDecimal frozenBefore) { this.frozenBefore = frozenBefore; }
  /**
   * 获取FrozenAfter。
   *
   * @return 获取FrozenAfter的结果
   */
  public BigDecimal getFrozenAfter() { return frozenAfter; }
  /**
   * frozenAfter。
   */
  /**
   * 设置FrozenAfter。
   *
   * @param frozenAfter frozenAfter
   */
  public void setFrozenAfter(BigDecimal frozenAfter) { this.frozenAfter = frozenAfter; }
  /**
   * 获取备注。
   *
   * @return 获取备注的结果
   */
  public String getRemark() { return remark; }
  /**
   * 备注。
   */
  /**
   * 设置备注。
   *
   * @param remark 备注
   */
  public void setRemark(String remark) { this.remark = remark; }
  /**
   * 获取创建时间。
   *
   * @return 获取创建时间的结果
   */
  public LocalDateTime getCreatedAt() { return createdAt; }
  /**
   * 创建时间。
   */
  /**
   * 设置创建时间。
   *
   * @param createdAt 创建时间
   */
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
