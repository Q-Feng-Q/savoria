package com.familykitchen.wallet.model.bo;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.wallet.model.enums.LedgerType;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 家庭成员钱包业务对象。
 */
public class WalletAccount {

  /**
   * 成员标识。
   */
  private final Long memberId;
  /**
   * 余额金额。
   */
  private BigDecimal balanceAmount;
  /**
   * frozen金额。
   */
  private BigDecimal frozenAmount;

  /**
   * frozen金额。
   */
  /**
   * 余额金额。
   */
  /**
   * 成员标识。
   */
  /**
   * 创建钱包Account实例。
   *
   * @param memberId 成员标识
   * @param balanceAmount 余额金额
   * @param frozenAmount frozen金额
   */
  public WalletAccount(Long memberId, BigDecimal balanceAmount, BigDecimal frozenAmount) {
    this.memberId = memberId;
    this.balanceAmount = money(balanceAmount);
    this.frozenAmount = money(frozenAmount);
  }

  /**
   * 金额。
   */
  /**
   * 处理钱包Account。
   *
   * @param amount 金额
   * @return 处理的结果
   */
  public WalletChange freeze(BigDecimal amount) {
    /**
     * normalized。
     */
    BigDecimal normalized = positive(amount);
    if (balanceAmount.compareTo(normalized) < 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "钱包余额不足");
    }
    /**
     * 余额Before。
     */
    BigDecimal balanceBefore = balanceAmount;
    /**
     * frozenBefore。
     */
    BigDecimal frozenBefore = frozenAmount;
    balanceAmount = balanceAmount.subtract(normalized);
    frozenAmount = frozenAmount.add(normalized);
    return change(LedgerType.FREEZE, normalized, balanceBefore, frozenBefore);
  }

  /**
   * 金额。
   */
  /**
   * 处理钱包Account。
   *
   * @param amount 金额
   * @return 处理的结果
   */
  public WalletChange release(BigDecimal amount) {
    /**
     * normalized。
     */
    BigDecimal normalized = positive(amount);
    if (frozenAmount.compareTo(normalized) < 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "冻结金额不足");
    }
    /**
     * 余额Before。
     */
    BigDecimal balanceBefore = balanceAmount;
    /**
     * frozenBefore。
     */
    BigDecimal frozenBefore = frozenAmount;
    balanceAmount = balanceAmount.add(normalized);
    frozenAmount = frozenAmount.subtract(normalized);
    return change(LedgerType.RELEASE, normalized, balanceBefore, frozenBefore);
  }

  /**
   * 金额。
   */
  /**
   * 处理钱包Account。
   *
   * @param amount 金额
   * @return 处理的结果
   */
  public WalletChange settle(BigDecimal amount) {
    /**
     * normalized。
     */
    BigDecimal normalized = positive(amount);
    if (frozenAmount.compareTo(normalized) < 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "冻结金额不足");
    }
    /**
     * 余额Before。
     */
    BigDecimal balanceBefore = balanceAmount;
    /**
     * frozenBefore。
     */
    BigDecimal frozenBefore = frozenAmount;
    frozenAmount = frozenAmount.subtract(normalized);
    return change(LedgerType.SETTLE, normalized, balanceBefore, frozenBefore);
  }

  /**
   * 金额。
   */
  /**
   * 处理Credit。
   *
   * @param amount 金额
   * @return 处理Credit的结果
   */
  public WalletChange manualCredit(BigDecimal amount) {
    return increase(LedgerType.MANUAL_CREDIT, amount);
  }

  /**
   * 金额。
   */
  /**
   * 处理Recharge。
   *
   * @param amount 金额
   * @return 处理Recharge的结果
   */
  public WalletChange merchantRecharge(BigDecimal amount) {
    return increase(LedgerType.MERCHANT_RECHARGE, amount);
  }

  /**
   * 金额。
   */
  /**
   * 处理Debit。
   *
   * @param amount 金额
   * @return 处理Debit的结果
   */
  public WalletChange manualDebit(BigDecimal amount) {
    /**
     * normalized。
     */
    BigDecimal normalized = positive(amount);
    if (balanceAmount.compareTo(normalized) < 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "钱包余额不足");
    }
    /**
     * 余额Before。
     */
    BigDecimal balanceBefore = balanceAmount;
    /**
     * frozenBefore。
     */
    BigDecimal frozenBefore = frozenAmount;
    balanceAmount = balanceAmount.subtract(normalized);
    return change(LedgerType.MANUAL_DEBIT, normalized, balanceBefore, frozenBefore);
  }

  /**
   * 处理标识。
   *
   * @return 处理标识的结果
   */
  public Long memberId() {
    return memberId;
  }

  /**
   * 处理金额。
   *
   * @return 处理金额的结果
   */
  public BigDecimal balanceAmount() {
    return balanceAmount;
  }

  /**
   * 处理金额。
   *
   * @return 处理金额的结果
   */
  public BigDecimal frozenAmount() {
    return frozenAmount;
  }

  private WalletChange change(
      /**
       * 类型。
       */
      LedgerType type,
      /**
       * 金额。
       */
      BigDecimal amount,
      /**
       * 余额Before。
       */
      BigDecimal balanceBefore,
      /**
       * frozenBefore。
       */
      BigDecimal frozenBefore
  ) {
    return new WalletChange(
        memberId,
        type,
        amount,
        balanceBefore,
        balanceAmount,
        frozenBefore,
        frozenAmount
    );
  }

  /**
   * 金额。
   */
  /**
   * 类型。
   */
  private WalletChange increase(LedgerType type, BigDecimal amount) {
    /**
     * normalized。
     */
    BigDecimal normalized = positive(amount);
    /**
     * 余额Before。
     */
    BigDecimal balanceBefore = balanceAmount;
    /**
     * frozenBefore。
     */
    BigDecimal frozenBefore = frozenAmount;
    balanceAmount = balanceAmount.add(normalized);
    return change(type, normalized, balanceBefore, frozenBefore);
  }

  /**
   * 金额。
   */
  private static BigDecimal positive(BigDecimal amount) {
    /**
     * normalized。
     */
    BigDecimal normalized = money(amount);
    if (normalized.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "金额不能为负数");
    }
    return normalized;
  }

  /**
   * 金额。
   */
  private static BigDecimal money(BigDecimal amount) {
    if (amount == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "金额不能为空");
    }
    return amount.setScale(2, RoundingMode.HALF_UP);
  }
}
