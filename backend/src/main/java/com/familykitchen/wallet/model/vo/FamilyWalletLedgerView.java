package com.familykitchen.wallet.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable family-wallet audit entry.
 * @param ledgerId ledger identifier
 * @param familyId family identifier
 * @param orderId related order identifier
 * @param operatorUserId operator identifier
 * @param businessType mutation type
 * @param amount absolute mutation amount
 * @param availableBefore available before
 * @param availableAfter available after
 * @param frozenBefore frozen before
 * @param frozenAfter frozen after
 * @param remark audit remark
 * @param createdAt creation time
 */
public record FamilyWalletLedgerView(Long ledgerId, Long familyId, Long orderId,
    Long operatorUserId, String businessType, BigDecimal amount, BigDecimal availableBefore,
    BigDecimal availableAfter, BigDecimal frozenBefore, BigDecimal frozenAfter,
    String remark, LocalDateTime createdAt) {}
