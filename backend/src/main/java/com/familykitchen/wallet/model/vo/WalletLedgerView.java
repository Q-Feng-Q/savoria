package com.familykitchen.wallet.model.vo;

import com.familykitchen.wallet.model.enums.LedgerType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 封装返回给调用方的钱包Ledger数据。
 *
 * @param ledgerId ledger标识
 * @param memberId 成员标识
 * @param type 类型
 * @param amount 金额
 * @param balanceBefore 余额Before
 * @param balanceAfter 余额After
 * @param frozenBefore frozenBefore
 * @param frozenAfter frozenAfter
 * @param remark 备注
 * @param createdAt 创建时间
 */
public record WalletLedgerView(
    Long ledgerId,
    Long memberId,
    LedgerType type,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    BigDecimal frozenBefore,
    BigDecimal frozenAfter,
    String remark,
    LocalDateTime createdAt
) {
}


