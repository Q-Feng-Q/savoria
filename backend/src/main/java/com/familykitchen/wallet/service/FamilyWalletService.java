package com.familykitchen.wallet.service;

import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import java.math.BigDecimal;

/** Coordinates locked family-wallet and order-hold transitions. */
public interface FamilyWalletService {
  /** Gets an account snapshot.
   * @param familyId family identifier
   * @return account snapshot
   */
  FamilyWalletAccountDO get(long familyId);
  /** Creates an initial order hold.
   * @param familyId family
   * @param orderId order
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   */
  void freezeNewOrder(long familyId,long orderId,long actorId,BigDecimal amount,String businessKey);
  /** Appends an order hold.
   * @param familyId family
   * @param orderId order
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   */
  void appendFreeze(long familyId,long orderId,long actorId,BigDecimal amount,String businessKey);
  /** Releases an order hold.
   * @param familyId family
   * @param orderId order
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   */
  void release(long familyId,long orderId,long actorId,BigDecimal amount,String businessKey);
  /** Captures an order hold.
   * @param familyId family
   * @param orderId order
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   */
  void capture(long familyId,long orderId,long actorId,BigDecimal amount,String businessKey);
  /** Refunds captured order money.
   * @param familyId family
   * @param orderId order
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   */
  void refund(long familyId,long orderId,long actorId,BigDecimal amount,String businessKey);
  /** Credits the wallet manually.
   * @param familyId family
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   * @param remark audit remark
   */
  void manualCredit(long familyId,long actorId,BigDecimal amount,String businessKey,String remark);
  /** Debits the wallet manually.
   * @param familyId family
   * @param actorId actor
   * @param amount amount
   * @param businessKey unique key
   * @param remark audit remark
   */
  void manualDebit(long familyId,long actorId,BigDecimal amount,String businessKey,String remark);
}
