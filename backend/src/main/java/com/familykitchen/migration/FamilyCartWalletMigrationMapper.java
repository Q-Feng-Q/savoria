package com.familykitchen.migration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists migration batches, source snapshots, cutover state, and family data movement. */
@Mapper
public interface FamilyCartWalletMigrationMapper {
  /** Maps a migration persistence operation.
   *
   * @return current database name */
  String currentDatabase();
  /** Creates a preflight batch. */
  void createBatch();
  /** Maps a migration persistence operation.
   *
   * @return connection-local generated identifier */
  Long lastInsertId();
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return locked batch row */
  Map<String, Object> lockBatch(long batchId);
  /** Maps a migration persistence operation.
   *
   * @return locked global cutover row */
  Map<String, Object> lockCutover();
  /** Ensures the global cutover row exists. */
  void ensureCutover();
  /** Maps a migration persistence operation.
   *
   * @return unexpired web-lease count */
  int countActiveLeases();
  /** Maps a migration persistence operation.
   *
   * @return next monotonic drain epoch */
  long nextDrainEpoch();
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @param owner runner owner */
  void enableBarrier(@Param("batchId") long batchId, @Param("epoch") long epoch,
      @Param("owner") String owner);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch */
  void markBatchQuiescing(@Param("batchId") long batchId, @Param("epoch") long epoch);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch */
  void markDrained(@Param("batchId") long batchId, @Param("epoch") long epoch);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch */
  void markBatchDrained(@Param("batchId") long batchId, @Param("epoch") long epoch);
  /** Clears the pre-execution write barrier. */
  void clearBarrier();
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return migrated wallet-source count */
  int countSources(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return migrated cart-source count */
  int countCartSources(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return recorded finalization-DDL count */
  int countFinalizationDdl(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier */
  void markAborted(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier */
  void clearAnomalies(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return inserted blocking-anomaly count */
  int insertPreflightAnomalies(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return inserted informational-report count */
  int insertPreflightReports(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return unresolved-anomaly count */
  int countAnomalies(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch optional drain epoch */
  void bindPreflight(@Param("batchId") long batchId, @Param("epoch") Long epoch);
  /** Maps a migration persistence operation.
   *
   * @return active family identifiers in deterministic order */
  List<Long> selectFamilies();
  /** Maps a migration persistence operation.
   *
   * @param familyId family identifier
   * @return locked personal-wallet sources */
  List<Map<String, Object>> selectWalletSources(long familyId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param userId user identifier
   * @param familyId family identifier
   * @param available available amount
   * @param frozen frozen amount
   * @return one when newly claimed, otherwise zero */
  int insertWalletSource(@Param("batchId") long batchId, @Param("userId") long userId,
      @Param("familyId") long familyId, @Param("available") BigDecimal available,
      @Param("frozen") BigDecimal frozen);
  /** Maps a migration persistence operation.
   *
   * @param familyId family identifier */
  void ensureFamilyWallet(long familyId);
  /** Maps a migration persistence operation.
   *
   * @param familyId family identifier
   * @param available available amount
   * @param frozen frozen amount */
  void creditFamilyWallet(@Param("familyId") long familyId, @Param("available") BigDecimal available,
      @Param("frozen") BigDecimal frozen);
  /** Maps a migration persistence operation.
   *
   * @param userId user identifier */
  void zeroMemberWallet(long userId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param userId user identifier
   * @param available available amount
   * @param frozen frozen amount */
  void insertTransferLedger(@Param("batchId") long batchId, @Param("userId") long userId,
      @Param("available") BigDecimal available, @Param("frozen") BigDecimal frozen);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param userId user identifier
   * @param familyId family identifier
   * @param available available amount
   * @param frozen frozen amount */
  void insertFamilyLedger(@Param("batchId") long batchId, @Param("userId") long userId,
      @Param("familyId") long familyId, @Param("available") BigDecimal available,
      @Param("frozen") BigDecimal frozen);
  /** Maps a migration persistence operation.
   *
   * @param familyId family identifier */
  void insertOrderHolds(long familyId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param familyId family identifier
   * @return unprocessed active carts */
  List<Map<String, Object>> selectActiveCarts(@Param("batchId") long batchId,
      @Param("familyId") long familyId);
  /** Maps a migration persistence operation.
   *
   * @param cartId cart identifier
   * @return cart item rows */
  List<Map<String, Object>> selectCartItems(long cartId);
  /** Maps a migration persistence operation.
   *
   * @param cartId cart identifier
   * @param dishId dish identifier
   * @return target item identifier */
  Long findTargetItem(@Param("cartId") long cartId, @Param("dishId") long dishId);
  /** Maps a migration persistence operation.
   *
   * @param itemId item identifier
   * @param quantity quantity delta
   * @param price current price
   * @param remark newest non-empty remark */
  void updateTargetItem(@Param("itemId") long itemId, @Param("quantity") int quantity,
      @Param("price") BigDecimal price, @Param("remark") String remark);
  /** Maps a migration persistence operation.
   *
   * @param cartId cart identifier
   * @param dishId dish identifier
   * @param name dish snapshot
   * @param quantity quantity
   * @param price current price
   * @param remark item remark */
  void insertTargetItem(@Param("cartId") long cartId, @Param("dishId") long dishId,
      @Param("name") String name, @Param("quantity") int quantity,
      @Param("price") BigDecimal price, @Param("remark") String remark);
  /** Maps a migration persistence operation.
   *
   * @param itemId item identifier
   * @param userId selection owner
   * @param quantity quantity
   * @param remark newest non-empty remark */
  void upsertSelection(@Param("itemId") long itemId, @Param("userId") long userId,
      @Param("quantity") int quantity, @Param("remark") String remark);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param sourceId source cart identifier
   * @param targetId target cart identifier
   * @param familyId family identifier */
  void markCartMigrated(@Param("batchId") long batchId, @Param("sourceId") long sourceId,
      @Param("targetId") long targetId, @Param("familyId") long familyId);
  /** Maps a migration persistence operation.
   *
   * @param cartId absorbed cart identifier */
  void absorbCart(long cartId);
  /** Maps a migration persistence operation.
   *
   * @param cartId cart identifier
   * @param remark basket remark */
  void updateTargetCart(@Param("cartId") long cartId, @Param("remark") String remark);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @return affected row count */
  int markExecuted(@Param("batchId") long batchId, @Param("epoch") long epoch);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return conservation totals */
  Map<String, Object> verificationTotals(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return invalid hold count */
  int countInvalidHolds(long batchId);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @param sourceAvailable source available total
   * @param sourceFrozen source frozen total
   * @param targetAvailable target available total
   * @param targetFrozen target frozen total
   * @return affected row count */
  int markVerified(@Param("batchId") long batchId, @Param("epoch") long epoch,
      @Param("sourceAvailable") BigDecimal sourceAvailable,
      @Param("sourceFrozen") BigDecimal sourceFrozen,
      @Param("targetAvailable") BigDecimal targetAvailable,
      @Param("targetFrozen") BigDecimal targetFrozen);
  /** Maps a migration persistence operation.
   *
   * @param indexName index name
   * @return matching index count */
  int indexExists(String indexName);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param type DDL audit type
   * @param detail audit detail */
  void recordDdl(@Param("batchId") long batchId, @Param("type") String type,
      @Param("detail") String detail);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @return affected row count */
  int setFamilyReady(@Param("batchId") long batchId, @Param("epoch") long epoch);
  /** Maps a migration persistence operation.
   *
   * @param batchId batch identifier
   * @return affected row count */
  int markBatchFinalized(@Param("batchId") long batchId);
  /** Maps a migration persistence operation.
   *
   * @param instanceId instance identifier
   * @param build build identifier
   * @param owner lease owner
   * @param seconds lease duration */
  void heartbeat(@Param("instanceId") String instanceId, @Param("build") String build,
      @Param("owner") String owner, @Param("seconds") long seconds);
  /** Maps a migration persistence operation.
   *
   * @param instanceId instance identifier */
  void removeLease(String instanceId);
  /**
   * Attempts to acquire the singleton persistent runner lease.
   *
   * @param ownerToken proposed owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   * @param mode migration mode
   * @param seconds lease duration in seconds
   * @return affected row count
   */
  int acquireRunnerLease(@Param("ownerToken") String ownerToken, @Param("batchId") long batchId,
      @Param("epoch") long epoch, @Param("mode") String mode, @Param("seconds") long seconds);
  /**
   * Locks and validates an unexpired runner lease.
   *
   * @param ownerToken expected owner token
   * @param batchId expected batch identifier
   * @param epoch expected drain epoch
   * @return locked lease row, or {@code null} when proof is stale
   */
  Map<String, Object> lockAndVerifyRunnerLease(@Param("ownerToken") String ownerToken,
      @Param("batchId") long batchId, @Param("epoch") long epoch);
  /**
   * Renews the exact unexpired runner lease.
   *
   * @param ownerToken expected owner token
   * @param batchId expected batch identifier
   * @param epoch expected drain epoch
   * @param seconds new lease duration in seconds
   * @return affected row count
   */
  int renewRunnerLease(@Param("ownerToken") String ownerToken, @Param("batchId") long batchId,
      @Param("epoch") long epoch, @Param("seconds") long seconds);
  /**
   * Releases the exact runner owner.
   *
   * @param ownerToken owner token
   * @return affected row count
   */
  int releaseRunnerLease(String ownerToken);
  /**
   * Checks whether the global business-write barrier is enabled.
   *
   * @return matching enabled-barrier count
   */
  int isWriteBarrierEnabled();
  /**
   * Snapshots all eligible active families for a batch.
   *
   * @param batchId batch identifier
   */
  void populateEligibleFamilies(long batchId);
  /**
   * Removes pending families that became ineligible before barrier preflight.
   *
   * @param batchId batch identifier
   */
  void removeIneligibleFamilies(long batchId);
  /**
   * Selects deterministic not-yet-migrated families.
   *
   * @param batchId batch identifier
   * @return pending family identifiers
   */
  List<Long> selectPendingFamilies(long batchId);
  /**
   * Selects every family snapshotted by a batch.
   *
   * @param batchId batch identifier
   * @return batch family identifiers
   */
  List<Long> selectBatchFamilies(long batchId);
  /**
   * Locks one batch-family progress row.
   *
   * @param batchId batch identifier
   * @param familyId family identifier
   * @return locked progress row
   */
  Map<String, Object> lockFamilyProgress(@Param("batchId") long batchId,
      @Param("familyId") long familyId);
  /**
   * Marks one pending family migrated.
   *
   * @param batchId batch identifier
   * @param familyId family identifier
   */
  void markFamilyMigrated(@Param("batchId") long batchId, @Param("familyId") long familyId);
  /**
   * Counts families that have not reached migrated status.
   *
   * @param batchId batch identifier
   * @return pending family count
   */
  int countPendingFamilies(long batchId);
  /**
   * Counts eligible member wallets that still contain money.
   *
   * @param batchId batch identifier
   * @return unmigrated wallet count
   */
  int countUnmigratedWallets(long batchId);
  /**
   * Counts active orders missing their exact migrated family-wallet hold.
   *
   * @param batchId batch identifier
   * @return missing or mismatched hold count
   */
  int countMissingExpectedHolds(long batchId);
  /**
   * Counts conservation and attribution failures for one family.
   *
   * @param batchId batch identifier
   * @param familyId family identifier
   * @return verification failure count
   */
  int countFamilyVerificationFailures(@Param("batchId") long batchId,
      @Param("familyId") long familyId);
  /**
   * Counts named cart-table columns.
   *
   * @param columnName column name
   * @return matching column count
   */
  int columnExists(@Param("columnName") String columnName);
  /**
   * Reads exact generated-column metadata.
   *
   * @param columnName column name
   * @return data type, extra flags, and generation expression
   */
  Map<String, Object> generatedColumnMetadata(@Param("columnName") String columnName);
  /**
   * Counts an exact single-column unique cart index.
   *
   * @param indexName index name
   * @param columnName required indexed column
   * @return one only for the exact unique single-column index
   */
  int exactUniqueIndexExists(@Param("indexName") String indexName,
      @Param("columnName") String columnName);
  /** Drops the legacy active-cart unique index. */
  void dropActiveCartIndex();
  /** Drops the legacy active-cart generated column. */
  void dropActiveCartColumn();
  /** Adds the final generated active-family column. */
  void addActiveFamilyColumn();
  /** Adds the final unique active-family index. */
  void addActiveFamilyIndex();
}
