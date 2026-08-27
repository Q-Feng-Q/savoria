package com.familykitchen.wallet.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable audit row for a family-wallet mutation. */
public class FamilyWalletLedgerDO {
  /** Ledger identifier. */ public Long id;
  /** Family identifier. */ public Long familyId;
  /** Related order identifier. */ public Long orderId;
  /** Operator user identifier. */ public Long operatorUserId;
  /** Normalized scope. */ public String scopeKey;
  /** Mutation type. */ public String businessType;
  /** Unique business key. */ public String businessKey;
  /** Audit remark. */ public String remark;
  /** Absolute mutation amount. */ public BigDecimal amount;
  /** Available balance before mutation. */ public BigDecimal availableBefore;
  /** Available balance after mutation. */ public BigDecimal availableAfter;
  /** Frozen balance before mutation. */ public BigDecimal frozenBefore;
  /** Frozen balance after mutation. */ public BigDecimal frozenAfter;
  /** Creation time. */ public LocalDateTime createdAt;
}
