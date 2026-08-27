package com.familykitchen.migration;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Executes each irreversible cart DDL step independently and idempotently. */
@Service
public class FamilyCartWalletDdlExecutor {
  private final FamilyCartWalletMigrationMapper mapper;
  private final FamilyWalletMigrationLeaseService leases;

  /**
   * Creates the resumable finalization executor.
   *
   * @param mapper migration persistence mapper
   * @param leases persistent runner lease service
   */
  public FamilyCartWalletDdlExecutor(FamilyCartWalletMigrationMapper mapper,
      FamilyWalletMigrationLeaseService leases) {
    this.mapper = mapper;
    this.leases = leases;
  }

  /**
   * Drops the legacy active-cart unique index when it still exists.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch verified drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void dropLegacyIndex(String token, long batchId, long epoch) {
    requireProof(token, batchId, epoch);
    marker(batchId, "DDL_DROP_LEGACY_INDEX_BEFORE", "uk_carts_active_cart");
    if (mapper.indexExists("uk_carts_active_cart") != 0) mapper.dropActiveCartIndex();
    if (mapper.indexExists("uk_carts_active_cart") != 0) throw new IllegalStateException("legacy cart index remains");
    marker(batchId, "DDL_DROP_LEGACY_INDEX_AFTER", "confirmed");
  }

  /**
   * Drops the legacy generated active-cart column when it still exists.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch verified drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void dropLegacyColumn(String token, long batchId, long epoch) {
    requireProof(token, batchId, epoch);
    marker(batchId, "DDL_DROP_LEGACY_COLUMN_BEFORE", "active_cart_key");
    if (mapper.columnExists("active_cart_key") != 0) mapper.dropActiveCartColumn();
    if (mapper.columnExists("active_cart_key") != 0) throw new IllegalStateException("legacy cart column remains");
    marker(batchId, "DDL_DROP_LEGACY_COLUMN_AFTER", "confirmed");
  }

  /**
   * Adds and validates the generated active-family column.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch verified drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void addFamilyColumn(String token, long batchId, long epoch) {
    requireProof(token, batchId, epoch);
    marker(batchId, "DDL_ADD_FAMILY_COLUMN_BEFORE", "active_family_id");
    if (mapper.columnExists("active_family_id") == 0) mapper.addActiveFamilyColumn();
    String expression = mapper.generatedColumnExpression("active_family_id");
    if (expression == null || !expression.toLowerCase().contains("family_id")
        || !expression.toLowerCase().contains("status")) {
      throw new IllegalStateException("active_family_id expression mismatch");
    }
    marker(batchId, "DDL_ADD_FAMILY_COLUMN_AFTER", expression);
  }

  /**
   * Adds and validates the unique active-family index.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch verified drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void addFamilyIndex(String token, long batchId, long epoch) {
    requireProof(token, batchId, epoch);
    marker(batchId, "DDL_ADD_FAMILY_INDEX_BEFORE", "uk_carts_active_family(active_family_id)");
    if (mapper.indexExists("uk_carts_active_family") == 0) mapper.addActiveFamilyIndex();
    if (mapper.indexExists("uk_carts_active_family") != 1
        || !"active_family_id".equalsIgnoreCase(mapper.indexColumn("uk_carts_active_family"))) {
      throw new IllegalStateException("family cart index mismatch");
    }
    marker(batchId, "DDL_ADD_FAMILY_INDEX_AFTER", "confirmed");
  }

  /**
   * Publishes the family-ready cutover after exact DDL inventory validation.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch verified drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cutover(String token, long batchId, long epoch) {
    requireProof(token, batchId, epoch);
    if (mapper.indexExists("uk_carts_active_cart") != 0 || mapper.columnExists("active_cart_key") != 0
        || mapper.columnExists("active_family_id") != 1 || mapper.indexExists("uk_carts_active_family") != 1
        || !"active_family_id".equalsIgnoreCase(mapper.indexColumn("uk_carts_active_family"))) {
      throw new IllegalStateException("final cart DDL inventory is incomplete");
    }
    if (mapper.setFamilyReady(batchId, epoch) != 1) {
      throw new IllegalStateException("cutover state changed before finalization");
    }
    if (mapper.markBatchFinalized(batchId) != 1) throw new IllegalStateException("finalize transition rejected");
  }

  private void requireProof(String token, long batchId, long epoch) {
    leases.renewForDdl(token, batchId, epoch);
    leases.requireOwned(token, batchId, epoch);
    Map<String, Object> batch = mapper.lockBatch(batchId);
    if (batch == null || !"VERIFIED".equals(text(batch.get("status")))) {
      throw new IllegalStateException("only a verified batch may finalize");
    }
    Map<String, Object> cutover = mapper.lockCutover();
    if (cutover == null || !truth(cutover.get("maintenanceEnabled"))
        || !"DRAINED".equals(text(cutover.get("state")))
        || number(cutover.get("activeBatchId")) != batchId
        || number(cutover.get("drainEpoch")) != epoch || mapper.countActiveLeases() != 0) {
      throw new IllegalStateException("current drain proof is required before DDL");
    }
  }

  private void marker(long batchId, String type, String detail) { mapper.recordDdl(batchId, type, detail); }
  private static String text(Object value) { return value == null ? "" : value.toString(); }
  private static long number(Object value) { return value instanceof Number n ? n.longValue() : -1; }
  private static boolean truth(Object value) {
    return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
  }
}
