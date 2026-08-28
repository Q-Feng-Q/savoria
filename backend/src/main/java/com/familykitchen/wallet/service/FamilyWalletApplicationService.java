package com.familykitchen.wallet.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.wallet.model.dto.AdjustFamilyBalanceRequest;
import com.familykitchen.wallet.model.vo.FamilyWalletLedgerView;
import com.familykitchen.wallet.model.vo.FamilyWalletSummaryView;
import java.util.List;

/** Authorizes family and merchant wallet HTTP use cases. */
public interface FamilyWalletApplicationService {
  /** Returns the current family's wallet.
   * @param user family user
   * @return current family wallet
   */
  FamilyWalletSummaryView summary(CurrentUserContext user);
  /** Returns one page of current-family wallet ledgers.
   * @param user family user
   * @param page one-based page
   * @param pageSize page size
   * @return ledgers
   */
  List<FamilyWalletLedgerView> ledgers(CurrentUserContext user, int page, int pageSize);
  /** Returns a merchant-owned family's wallet.
   * @param user merchant user
   * @param familyId family
   * @return wallet
   */
  FamilyWalletSummaryView summaryForMerchant(CurrentUserContext user, Long familyId);
  /** Returns a merchant-owned family's ledger page.
   * @param user merchant user
   * @param familyId family
   * @param page page
   * @param pageSize size
   * @return ledgers
   */
  List<FamilyWalletLedgerView> ledgersForMerchant(
      CurrentUserContext user, Long familyId, int page, int pageSize);
  /** Applies an idempotent merchant family-wallet adjustment.
   * @param user merchant user
   * @param familyId family
   * @param request command
   * @return updated wallet
   */
  FamilyWalletSummaryView adjustForMerchant(
      CurrentUserContext user, Long familyId, AdjustFamilyBalanceRequest request);
}
