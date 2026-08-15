package com.familykitchen.wallet.model.enums;

/**
 * 定义Ledger类型可使用的状态或类型。
 */
public enum LedgerType {
  /**
   * 表示 {@code MERCHANT_RECHARGE} 对应的业务取值。
   */
  MERCHANT_RECHARGE,
  /**
   * 表示 {@code MANUAL_CREDIT} 对应的业务取值。
   */
  MANUAL_CREDIT,
  /**
   * 表示 {@code MANUAL_DEBIT} 对应的业务取值。
   */
  MANUAL_DEBIT,
  /**
   * 表示 {@code FREEZE} 对应的业务取值。
   */
  FREEZE,
  /**
   * 表示 {@code RELEASE} 对应的业务取值。
   */
  RELEASE,
  /**
   * 表示 {@code SETTLE} 对应的业务取值。
   */
  SETTLE
}


