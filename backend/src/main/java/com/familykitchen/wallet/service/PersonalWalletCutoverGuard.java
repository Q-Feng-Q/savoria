package com.familykitchen.wallet.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import org.springframework.stereotype.Service;

/** Blocks family-wallet ordering while legacy personal money remains unreconciled. */
@Service
public class PersonalWalletCutoverGuard {
  private final WalletPersistenceMapper wallets;
  /** Creates the cutover guard.
   * @param wallets legacy wallet diagnostics
   */
  public PersonalWalletCutoverGuard(WalletPersistenceMapper wallets) { this.wallets = wallets; }
  /** Requires every active member personal wallet to be zero.
   * @param familyId family identifier
   */
  public void requireReady(Long familyId) {
    if (familyId != null && wallets.countNonZeroPersonalWallets(familyId) > 0) {
      throw new BusinessException(ErrorCode.CLIENT_UPGRADE_REQUIRED,
          "家庭成员个人钱包余额尚未清零，请先完成家庭钱包迁移核对");
    }
  }
}
