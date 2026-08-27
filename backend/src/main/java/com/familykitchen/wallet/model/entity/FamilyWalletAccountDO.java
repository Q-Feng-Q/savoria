package com.familykitchen.wallet.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Persistence row for {@code family_wallets}. */
public class FamilyWalletAccountDO {
  /** Family identifier. */ public Long familyId;
  /** Spendable amount. */ public BigDecimal availableAmount;
  /** Frozen amount. */ public BigDecimal frozenAmount;
  /** Mutation version. */ public Long version;
  /** Creation time. */ public LocalDateTime createdAt;
  /** Last update time. */ public LocalDateTime updatedAt;
}
