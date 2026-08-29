package com.familykitchen.wallet.service;

import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Creates missing wallet accounts before callers enter their locking transaction. */
@Service
public class FamilyWalletAccountInitializer {
  private final FamilyWalletMapper mapper;

  /**
   * Creates the initializer.
   *
   * @param mapper family-wallet persistence mapper
   */
  public FamilyWalletAccountInitializer(FamilyWalletMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Ensures an active family has a wallet account and commits it independently.
   *
   * @param familyId family identifier
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void ensure(long familyId) {
    if (mapper.selectAccount(familyId) == null) {
      mapper.insertAccountIfMissing(familyId);
    }
  }
}
