package com.familykitchen.wallet.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Family wallet balance snapshot.
 * @param familyId family identifier
 * @param availableAmount spendable balance
 * @param frozenAmount order-authorized balance
 * @param totalAmount available plus frozen
 * @param version mutation version
 * @param updatedAt last mutation time
 */
public record FamilyWalletSummaryView(Long familyId, BigDecimal availableAmount,
    BigDecimal frozenAmount, BigDecimal totalAmount, Long version, LocalDateTime updatedAt) {}
