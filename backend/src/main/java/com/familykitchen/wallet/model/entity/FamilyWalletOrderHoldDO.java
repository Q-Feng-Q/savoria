package com.familykitchen.wallet.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Persistence state machine for money frozen by one order. */
public class FamilyWalletOrderHoldDO {
  /** Hold identifier. */ public Long id;
  /** Order identifier. */ public Long orderId;
  /** Family identifier. */ public Long familyId;
  /** Initially frozen amount. */ public BigDecimal initialAmount;
  /** Additional frozen amount. */ public BigDecimal additionalFrozenAmount;
  /** Remaining frozen amount. */ public BigDecimal remainingFrozenAmount;
  /** Captured amount. */ public BigDecimal capturedAmount;
  /** Released amount. */ public BigDecimal releasedAmount;
  /** Refunded amount. */ public BigDecimal refundedAmount;
  /** Hold state. */ public String status;
  /** Creation time. */ public LocalDateTime createdAt;
  /** Last update time. */ public LocalDateTime updatedAt;
}
