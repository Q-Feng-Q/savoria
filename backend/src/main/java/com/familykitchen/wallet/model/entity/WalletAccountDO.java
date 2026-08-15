package com.familykitchen.wallet.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import org.apache.ibatis.type.Alias;

/**
 * 成员钱包账户持久化实体。
 *
 * <p>对应 `member_wallets` 表，保存成员可用余额和冻结金额。</p>
 */
@Alias("walletAccountDO")
@TableName("member_wallets")
public class WalletAccountDO {

  /** 成员 ID。 */
  @TableId(value = "user_id", type = IdType.INPUT)
  private Long memberId;

  /** 可用余额。 */
  @TableField("balance_amount")
  private BigDecimal balanceAmount;

  /** 冻结金额。 */
  @TableField("frozen_amount")
  private BigDecimal frozenAmount;

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
   * 获取余额金额。
   *
   * @return 获取余额金额的结果
   */
  public BigDecimal getBalanceAmount() { return balanceAmount; }
  /**
   * 余额金额。
   */
  /**
   * 设置余额金额。
   *
   * @param balanceAmount 余额金额
   */
  public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }
  /**
   * 获取Frozen金额。
   *
   * @return 获取Frozen金额的结果
   */
  public BigDecimal getFrozenAmount() { return frozenAmount; }
  /**
   * frozen金额。
   */
  /**
   * 设置Frozen金额。
   *
   * @param frozenAmount frozen金额
   */
  public void setFrozenAmount(BigDecimal frozenAmount) { this.frozenAmount = frozenAmount; }
}
