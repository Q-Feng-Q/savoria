package com.familykitchen.migration;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Commits barrier publication separately from lease drain proof. */
@Service
public class FamilyWalletMigrationBarrierService {
  private final FamilyCartWalletMigrationMapper mapper;
  private final FamilyWalletMigrationLeaseService leases;

  /**
   * Creates the durable write-barrier coordinator.
   *
   * @param mapper migration persistence mapper
   * @param leases persistent runner lease service
   */
  public FamilyWalletMigrationBarrierService(FamilyCartWalletMigrationMapper mapper,
      FamilyWalletMigrationLeaseService leases) {
    this.mapper = mapper;
    this.leases = leases;
  }

  /**
   * Publishes the barrier in an independent transaction.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param owner human-readable barrier owner
   * @return allocated or previously allocated drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public long enable(String token, long batchId, String owner) {
    leases.requireOwned(token, batchId, 0);
    Map<String, Object> batch = requireBatch(batchId);
    String batchStatus = text(batch.get("status"));
    if (!"PREFLIGHT".equals(batchStatus) && !"QUIESCING".equals(batchStatus)) {
      throw new IllegalStateException("QUIESCE requires PREFLIGHT or QUIESCING");
    }
    mapper.ensureCutover();
    Map<String, Object> cutover = mapper.lockCutover();
    if (Boolean.TRUE.equals(bool(cutover.get("maintenanceEnabled")))) {
      long epoch = number(cutover, "drainEpoch");
      if (!"QUIESCING".equals(text(cutover.get("state")))
          || number(cutover, "activeBatchId") != batchId || number(batch, "drainEpoch") != epoch) {
        throw new IllegalStateException("another cutover is already active");
      }
      return epoch;
    }
    long epoch = mapper.nextDrainEpoch();
    mapper.enableBarrier(batchId, epoch, owner);
    mapper.markBatchQuiescing(batchId, epoch);
    return epoch;
  }

  /**
   * Proves drain only after the barrier transaction is visible to every compatibility instance.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch to prove
   * @return {@code true} when every compatibility instance lease has drained
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean proveDrained(String token, long batchId, long epoch) {
    // QUIESCE owns epoch zero; the persisted cutover/batch epoch is checked under lock below.
    leases.requireOwned(token, batchId, 0);
    Map<String, Object> batch = requireBatch(batchId);
    Map<String, Object> cutover = mapper.lockCutover();
    if (!Boolean.TRUE.equals(bool(cutover.get("maintenanceEnabled")))
        || !"QUIESCING".equals(text(cutover.get("state")))
        || number(cutover, "activeBatchId") != batchId || number(cutover, "drainEpoch") != epoch
        || number(batch, "drainEpoch") != epoch) {
      throw new IllegalStateException("stale barrier drain proof");
    }
    if (mapper.countActiveLeases() != 0) return false;
    mapper.markDrained(batchId, epoch);
    mapper.markBatchDrained(batchId, epoch);
    return true;
  }

  private Map<String, Object> requireBatch(long batchId) {
    Map<String, Object> row = mapper.lockBatch(batchId);
    if (row == null) throw new IllegalStateException("unknown or stale batch id");
    return row;
  }

  private static void requireStatus(Map<String, Object> row, String expected) {
    if (!expected.equals(text(row.get("status")))) {
      throw new IllegalStateException("illegal migration phase: expected " + expected);
    }
  }

  private static long number(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value instanceof Number number ? number.longValue() : -1;
  }

  private static String text(Object value) { return value == null ? "" : value.toString(); }
  private static Boolean bool(Object value) {
    return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
  }
}
