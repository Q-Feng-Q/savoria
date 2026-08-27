package com.familykitchen.wallet.mapper;

import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.model.entity.FamilyWalletLedgerDO;
import com.familykitchen.wallet.model.entity.FamilyWalletOrderHoldDO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists family-wallet accounts, holds, and immutable ledgers. */
@Mapper
public interface FamilyWalletMapper {
  /** Selects an account.
   * @param familyId family identifier
   * @return account or null
   */
  FamilyWalletAccountDO selectAccount(long familyId);
  /** Locks an account.
   * @param familyId family identifier
   * @return locked account or null
   */
  FamilyWalletAccountDO lockAccount(long familyId);
  /** Locks an order hold.
   * @param orderId order identifier
   * @return locked hold or null
   */
  FamilyWalletOrderHoldDO lockHold(long orderId);
  /** Inserts an order hold.
   * @param hold new hold
   * @return affected rows
   */
  int insertHold(FamilyWalletOrderHoldDO hold);
  /** Updates an order hold.
   * @param hold changed hold
   * @return affected rows
   */
  int updateHold(FamilyWalletOrderHoldDO hold);
  /** Updates a locked account.
   * @param familyId family identifier
   * @param available spendable amount
   * @param frozen frozen amount
   * @return affected rows
   */
  int updateAccount(@Param("familyId") long familyId,
      @Param("available") BigDecimal available, @Param("frozen") BigDecimal frozen);
  /** Inserts an audit ledger.
   * @param ledger audit row
   * @return affected rows
   */
  int insertLedger(FamilyWalletLedgerDO ledger);
  /** Lists wallet ledgers.
   * @param familyId family identifier
   * @return newest-first ledgers
   */
  List<FamilyWalletLedgerDO> selectLedgers(long familyId);
}
