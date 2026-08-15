package com.familykitchen.wallet.model.bo;

import com.familykitchen.wallet.model.enums.LedgerType;

import java.math.BigDecimal;

/**
 * 表示钱包Change领域计算过程中的业务数据。
 *
 * @param memberId 成员标识
 * @param type 类型
 * @param amount 金额
 * @param balanceBefore 余额Before
 * @param balanceAfter 余额After
 * @param frozenBefore frozenBefore
 * @param frozenAfter frozenAfter
 */
public record WalletChange(
    Long memberId,
    LedgerType type,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    BigDecimal frozenBefore,
    BigDecimal frozenAfter
) {
}


