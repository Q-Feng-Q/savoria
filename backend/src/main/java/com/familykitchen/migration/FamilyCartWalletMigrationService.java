package com.familykitchen.migration;

import com.familykitchen.migration.FamilyCartWalletMigrationRunner.Mode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable migration operations. Runner orchestration deliberately calls the proxied family method. */
@Service
public class FamilyCartWalletMigrationService {
  private static final String GLOBAL = "GLOBAL";
  private final FamilyCartWalletMigrationMapper mapper;
  private final FamilyCartWalletFamilyExecutor familyExecutor;

  /**
   * Creates the durable migration service.
   *
   * @param mapper migration mapper
   * @param familyExecutor independent family transaction executor
   */
  public FamilyCartWalletMigrationService(FamilyCartWalletMigrationMapper mapper,
      FamilyCartWalletFamilyExecutor familyExecutor) {
    this.mapper = mapper;
    this.familyExecutor = familyExecutor;
  }

  /**
   * Deterministic phase result printed for operators.
   *
   * @param batchId batch identifier
   * @param drainEpoch drain epoch, or zero before quiesce
   * @param status resulting phase status
   * @param anomalyCount anomaly count
   */
  public record Result(long batchId, long drainEpoch, String status, int anomalyCount) { }

  /**
   * Creates or refreshes a preflight report.
   *
   * @param requestedBatchId existing batch identifier, or {@code null}
   * @param drainEpoch optional drain epoch for the barrier recheck
   * @return preflight result
   */
  @Transactional
  public Result preflight(Long requestedBatchId, Long drainEpoch) {
    long batchId;
    if (requestedBatchId == null) {
      mapper.createBatch();
      batchId = mapper.lastInsertId();
    } else {
      batchId = requestedBatchId;
    }
    Map<String, Object> batch = requireBatch(batchId);
    if (requestedBatchId != null) requireEpoch(batch, drainEpoch);
    mapper.clearAnomalies(batchId);
    int anomalies = mapper.insertPreflightAnomalies(batchId) + mapper.insertPreflightReports(batchId);
    mapper.bindPreflight(batchId, drainEpoch);
    return new Result(batchId, drainEpoch == null ? 0 : drainEpoch, "PREFLIGHT", anomalies);
  }

  /**
   * Enables the barrier and advances to drained when all web leases expire.
   *
   * @param batchId batch identifier
   * @param owner runner owner
   * @return quiescing or drained result
   */
  @Transactional
  public Result quiesce(Long batchId, String owner) {
    Map<String, Object> batch = requireBatch(batchId);
    mapper.ensureCutover();
    Map<String, Object> cutover = mapper.lockCutover();
    long epoch;
    if (!Boolean.TRUE.equals(bool(cutover.get("maintenanceEnabled")))) {
      epoch = mapper.nextDrainEpoch();
      mapper.enableBarrier(batchId, epoch, owner);
      mapper.markBatchQuiescing(batchId, epoch);
    } else {
      epoch = number(cutover, "drainEpoch", -1).longValue();
      requireEpoch(batch, epoch);
      String state = text(cutover.get("state"));
      if (!"QUIESCING".equals(state) && !"DRAINED".equals(state)) {
        throw new IllegalStateException("another cutover is already active");
      }
    }
    if (mapper.countActiveLeases() != 0) {
      return new Result(batchId, epoch, "QUIESCING", 0);
    }
    mapper.markDrained(batchId, epoch);
    mapper.markBatchDrained(batchId, epoch);
    return new Result(batchId, epoch, "DRAINED", 0);
  }

  /**
   * Clears the barrier only before any wallet, cart, or DDL source is recorded.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @return aborted result
   */
  @Transactional
  public Result abortBeforeExecute(Long batchId, Long epoch) {
    Map<String, Object> batch = requireBatch(batchId);
    requireEpoch(batch, epoch);
    if (mapper.countSources(batchId) != 0 || mapper.countCartSources(batchId) != 0
        || mapper.countFinalizationDdl() != 0) {
      throw new IllegalStateException("migration can no longer be aborted");
    }
    mapper.clearBarrier();
    mapper.markAborted(batchId);
    return new Result(batchId, epoch, "ABORTED", 0);
  }

  /**
   * Validates the batch and current drain proof before a continuation phase.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @param mode requested continuation mode
   * @return active family identifiers
   */
  @Transactional(readOnly = true)
  public List<Long> validateContinuation(Long batchId, Long epoch, Mode mode) {
    Map<String, Object> batch = requireBatch(batchId);
    requireEpoch(batch, epoch);
    Map<String, Object> cutover = mapper.lockCutover();
    if (cutover == null || !Boolean.TRUE.equals(bool(cutover.get("maintenanceEnabled")))
        || !"DRAINED".equals(text(cutover.get("state")))) {
      throw new IllegalStateException("current unexpired drain proof is required");
    }
    if (mapper.countAnomalies(batchId) != 0 && mode == Mode.EXECUTE) {
      throw new IllegalStateException("unresolved preflight anomalies");
    }
    return mapper.selectFamilies();
  }

  /**
   * Executes one family through the independent transaction bean.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @param familyId family identifier
   * @return whether the family transaction completed
   */
  public boolean executeFamily(Long batchId, Long epoch, Long familyId) {
    validateContinuation(batchId, epoch, Mode.EXECUTE);
    return familyExecutor.execute(batchId, epoch, familyId);
  }

  /**
   * Records completion after every family transaction succeeds.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   */
  @Transactional
  public void markExecutionComplete(Long batchId, Long epoch) {
    validateContinuation(batchId, epoch, Mode.EXECUTE);
    mapper.markExecuted(batchId, epoch);
  }

  /**
   * Verifies money, source clearing, holds, selections, and anomalies.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @return verified result
   */
  @Transactional
  public Result verify(Long batchId, Long epoch) {
    validateContinuation(batchId, epoch, Mode.VERIFY);
    Map<String, Object> totals = mapper.verificationTotals(batchId);
    BigDecimal sourceAvailable = decimal(totals.get("sourceAvailable"));
    BigDecimal sourceFrozen = decimal(totals.get("sourceFrozen"));
    BigDecimal targetAvailable = decimal(totals.get("targetAvailable"));
    BigDecimal targetFrozen = decimal(totals.get("targetFrozen"));
    long nonzeroSources = number(totals, "nonzeroSources", 0).longValue();
    long badSelections = number(totals, "badSelections", 0).longValue();
    if (sourceAvailable.compareTo(targetAvailable) != 0 || sourceFrozen.compareTo(targetFrozen) != 0
        || nonzeroSources != 0 || badSelections != 0 || mapper.countInvalidHolds(batchId) != 0
        || mapper.countAnomalies(batchId) != 0) {
      throw new IllegalStateException("migration conservation verification failed");
    }
    mapper.markVerified(batchId, epoch, sourceAvailable, sourceFrozen, targetAvailable, targetFrozen);
    return new Result(batchId, epoch, "VERIFIED", 0);
  }

  /**
   * Installs resumable final DDL and advances the cutover to family-ready.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @return finalized result
   */
  @Transactional
  public Result finalizeBatch(Long batchId, Long epoch) {
    Map<String, Object> batch = requireBatch(batchId);
    requireEpoch(batch, epoch);
    if ("FINALIZED".equals(text(batch.get("status")))) {
      Map<String, Object> cutover = mapper.lockCutover();
      if (cutover != null && "FAMILY_READY".equals(text(cutover.get("state")))) {
        return new Result(batchId, epoch, "FINALIZED", 0);
      }
      throw new IllegalStateException("finalized batch has inconsistent cutover state");
    }
    validateContinuation(batchId, epoch, Mode.FINALIZE);
    if (!"VERIFIED".equals(text(batch.get("status"))) && !"FINALIZED".equals(text(batch.get("status")))) {
      throw new IllegalStateException("only a verified batch may finalize");
    }
    mapper.recordDdl(batchId, "DDL_BEFORE", "legacy=" + mapper.indexExists("uk_carts_active_cart")
        + ",family=" + mapper.indexExists("uk_carts_active_family"));
    if (mapper.indexExists("uk_carts_active_family") == 0) {
      mapper.recordDdl(batchId, "DDL_REQUIRED", "uk_carts_active_family");
      mapper.applyFinalCartIndex();
    }
    if (mapper.indexExists("uk_carts_active_family") == 0) {
      throw new IllegalStateException("family active-cart index was not installed");
    }
    mapper.recordDdl(batchId, "DDL_AFTER", "uk_carts_active_family=confirmed");
    if (mapper.setFamilyReady(batchId, epoch) != 1) {
      throw new IllegalStateException("cutover state changed before finalization");
    }
    mapper.markBatchFinalized(batchId);
    return new Result(batchId, epoch, "FINALIZED", 0);
  }

  /**
   * Reads the actual database selected by the datasource connection.
   *
   * @return current database name
   */
  @Transactional(readOnly = true)
  public String currentDatabase() {
    return mapper.currentDatabase();
  }

  private Map<String, Object> requireBatch(Long batchId) {
    if (batchId == null) throw new IllegalArgumentException("batch id required");
    Map<String, Object> batch = mapper.lockBatch(batchId);
    if (batch == null) throw new IllegalStateException("unknown or stale batch id");
    return batch;
  }

  private static void requireEpoch(Map<String, Object> batch, Long epoch) {
    if (epoch == null || number(batch, "drainEpoch", -1).longValue() != epoch) {
      throw new IllegalStateException("stale drain epoch");
    }
  }

  private static Number number(Map<String, Object> row, String key, Number fallback) {
    Object value = row.get(key);
    return value instanceof Number number ? number : fallback;
  }

  static BigDecimal decimal(Object value) {
    return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
  }

  static String text(Object value) { return value == null ? "" : value.toString(); }
  private static Boolean bool(Object value) {
    return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
  }
}
