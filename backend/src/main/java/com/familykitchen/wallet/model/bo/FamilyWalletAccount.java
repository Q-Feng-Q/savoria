package com.familykitchen.wallet.model.bo;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.math.BigDecimal;

/** Mutable family-wallet aggregate used while its database row is locked. */
public final class FamilyWalletAccount {
  /** Largest DECIMAL(18,2) value. */
  private static final BigDecimal MAX = new BigDecimal("9999999999999999.99");
  /** Family identifier. */
  private final Long familyId;
  /** Spendable amount. */
  private BigDecimal availableAmount;
  /** Frozen amount. */
  private BigDecimal frozenAmount;

  /** Creates a locked family-wallet aggregate.
   * @param familyId family identifier
   * @param availableAmount spendable amount
   * @param frozenAmount frozen amount
   */
  public FamilyWalletAccount(Long familyId, BigDecimal availableAmount, BigDecimal frozenAmount) {
    if (familyId == null || familyId <= 0) invalid("家庭 ID 不合法");
    this.familyId = familyId;
    this.availableAmount = balance(availableAmount);
    this.frozenAmount = balance(frozenAmount);
  }

  /** Freezes spendable money.
   * @param amount amount to freeze
   */
  public void freeze(BigDecimal amount) {
    BigDecimal value = command(amount);
    requireAtLeast(availableAmount, value, "家庭钱包可用余额不足");
    availableAmount = availableAmount.subtract(value);
    frozenAmount = checked(frozenAmount.add(value));
  }

  /** Releases frozen money.
   * @param amount amount to release
   */
  public void release(BigDecimal amount) {
    BigDecimal value = command(amount);
    requireAtLeast(frozenAmount, value, "家庭钱包冻结金额不足");
    frozenAmount = frozenAmount.subtract(value);
    availableAmount = checked(availableAmount.add(value));
  }

  /** Captures frozen money.
   * @param amount amount to capture
   */
  public void capture(BigDecimal amount) {
    BigDecimal value = command(amount);
    requireAtLeast(frozenAmount, value, "家庭钱包冻结金额不足");
    frozenAmount = frozenAmount.subtract(value);
  }

  /** Credits a captured refund.
   * @param amount refund amount
   */
  public void refund(BigDecimal amount) {
    availableAmount = checked(availableAmount.add(command(amount)));
  }

  /** Applies a manual credit.
   * @param amount credit amount
   */
  public void manualCredit(BigDecimal amount) {
    availableAmount = checked(availableAmount.add(command(amount)));
  }

  /** Applies a manual debit.
   * @param amount debit amount
   */
  public void manualDebit(BigDecimal amount) {
    BigDecimal value = command(amount);
    requireAtLeast(availableAmount, value, "家庭钱包可用余额不足");
    availableAmount = availableAmount.subtract(value);
  }

  /** Returns the family identifier.
   * @return family identifier
   */
  public Long familyId() { return familyId; }
  /** Returns the spendable amount.
   * @return spendable amount
   */
  public BigDecimal availableAmount() { return availableAmount; }
  /** Returns the frozen amount.
   * @return frozen amount
   */
  public BigDecimal frozenAmount() { return frozenAmount; }

  /** Validates an exact positive command amount.
   * @param amount supplied amount
   * @return normalized two-decimal amount
   */
  public static BigDecimal command(BigDecimal amount) {
    BigDecimal value = exact(amount);
    if (value.signum() <= 0) invalid("金额必须大于零");
    return value;
  }

  private static BigDecimal balance(BigDecimal amount) {
    BigDecimal value = exact(amount);
    if (value.signum() < 0) invalid("钱包金额不能为负数");
    return value;
  }

  private static BigDecimal exact(BigDecimal amount) {
    if (amount == null) invalid("金额不能为空");
    BigDecimal normalized = amount.stripTrailingZeros();
    if (Math.max(normalized.scale(), 0) > 2) invalid("金额最多保留两位小数");
    return checked(amount.setScale(2));
  }

  private static BigDecimal checked(BigDecimal amount) {
    if (amount.abs().compareTo(MAX) > 0) invalid("金额超出支持范围");
    return amount.setScale(2);
  }

  private static void requireAtLeast(BigDecimal actual, BigDecimal needed, String message) {
    if (actual.compareTo(needed) < 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, message);
    }
  }

  private static void invalid(String message) {
    throw new BusinessException(ErrorCode.BAD_REQUEST, message);
  }
}
