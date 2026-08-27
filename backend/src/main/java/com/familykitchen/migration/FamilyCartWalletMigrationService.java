package com.familykitchen.migration;

import com.familykitchen.migration.FamilyCartWalletMigrationRunner.Mode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable migration operations fenced by a persistent runner lease. */
@Service
public class FamilyCartWalletMigrationService {
  private final FamilyCartWalletMigrationMapper mapper;
  private final FamilyCartWalletFamilyExecutor familyExecutor;
  private final FamilyWalletMigrationLeaseService leases;
  private final FamilyWalletMigrationBarrierService barriers;
  private final FamilyCartWalletDdlExecutor ddl;

  /**
   * Creates the durable migration orchestrator.
   *
   * @param mapper migration persistence mapper
   * @param familyExecutor independently transactional family executor
   * @param leases persistent runner lease service
   * @param barriers durable write-barrier coordinator
   * @param ddl resumable finalization DDL executor
   */
  public FamilyCartWalletMigrationService(FamilyCartWalletMigrationMapper mapper,
      FamilyCartWalletFamilyExecutor familyExecutor, FamilyWalletMigrationLeaseService leases,
      FamilyWalletMigrationBarrierService barriers, FamilyCartWalletDdlExecutor ddl) {
    this.mapper = mapper;
    this.familyExecutor = familyExecutor;
    this.leases = leases;
    this.barriers = barriers;
    this.ddl = ddl;
  }

  /**
   * Reports one migration-mode outcome.
   *
   * @param batchId migration batch identifier
   * @param drainEpoch current drain epoch, or zero before drain
   * @param status resulting lifecycle status
   * @param anomalyCount blocking anomaly count
   */
  public record Result(long batchId, long drainEpoch, String status, int anomalyCount) { }

  /**
   * Creates or refreshes preflight reports for a batch.
   *
   * @param token runner owner token
   * @param requestedBatchId optional existing batch identifier
   * @param drainEpoch optional epoch for the required post-barrier preflight
   * @return preflight result
   */
  @Transactional
  public Result preflight(String token, Long requestedBatchId, Long drainEpoch) {
    long leaseBatch = requestedBatchId == null ? 0 : requestedBatchId;
    long leaseEpoch = drainEpoch == null ? 0 : drainEpoch;
    leases.requireOwned(token, leaseBatch, leaseEpoch);
    long batchId;
    if (requestedBatchId == null) {
      mapper.createBatch();
      batchId = mapper.lastInsertId();
    } else {
      batchId = requestedBatchId;
    }
    Map<String, Object> batch = requireBatch(batchId);
    String status = text(batch.get("status"));
    if (requestedBatchId == null && !"DRAFT".equals(status)) {
      throw new IllegalStateException("new preflight batch is not draft");
    }
    if (requestedBatchId != null) {
      if (drainEpoch != null) requireEpoch(batch, drainEpoch);
      if (drainEpoch == null && !"PREFLIGHT".equals(status)) {
        throw new IllegalStateException("initial preflight refresh requires PREFLIGHT");
      }
      if (drainEpoch != null && !"DRAINED".equals(status)) {
        throw new IllegalStateException("barrier preflight requires DRAINED");
      }
    }
    mapper.clearAnomalies(batchId);
    mapper.populateEligibleFamilies(batchId);
    int anomalies = mapper.insertPreflightAnomalies(batchId) + mapper.insertPreflightReports(batchId);
    mapper.bindPreflight(batchId, drainEpoch);
    return new Result(batchId, leaseEpoch, drainEpoch == null ? "PREFLIGHT" : "BARRIER_PREFLIGHT", anomalies);
  }

  /**
   * Publishes the write barrier and attempts to prove the compatibility fleet drained.
   *
   * @param token runner owner token
   * @param batchId preflight batch identifier
   * @param owner human-readable barrier owner
   * @return quiescing or drained result
   */
  public Result quiesce(String token, Long batchId, String owner) {
    if (batchId == null) throw new IllegalArgumentException("batch id required");
    long epoch = barriers.enable(token, batchId, owner);
    boolean drained = barriers.proveDrained(token, batchId, epoch);
    return new Result(batchId, epoch, drained ? "DRAINED" : "QUIESCING", 0);
  }

  /**
   * Clears a drained barrier only while no migration data or finalization DDL exists.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   * @return aborted result
   */
  @Transactional
  public Result abortBeforeExecute(String token, Long batchId, Long epoch) {
    leases.requireOwned(token, required(batchId), required(epoch));
    Map<String, Object> batch = requireBatch(batchId);
    requireEpoch(batch, epoch);
    requireOneOf(batch, "DRAINED", "BARRIER_PREFLIGHT");
    requireDrainProof(batchId, epoch);
    if (mapper.countSources(batchId) != 0 || mapper.countCartSources(batchId) != 0
        || mapper.countFinalizationDdl(batchId) != 0) {
      throw new IllegalStateException("migration can no longer be aborted");
    }
    mapper.clearBarrier();
    mapper.markAborted(batchId);
    return new Result(batchId, epoch, "ABORTED", 0);
  }

  /**
   * Validates lease, batch phase, epoch, barrier, drain, and anomaly continuation proof.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   * @param mode requested continuation mode
   * @return deterministic family identifiers for the requested phase
   */
  @Transactional
  public List<Long> validateContinuation(String token, Long batchId, Long epoch, Mode mode) {
    leases.requireOwned(token, required(batchId), required(epoch));
    Map<String, Object> batch = requireBatch(batchId);
    requireEpoch(batch, epoch);
    requireDrainProof(batchId, epoch);
    String status = text(batch.get("status"));
    if (mode == Mode.EXECUTE && !status.equals("BARRIER_PREFLIGHT") && !status.equals("EXECUTING")) {
      throw new IllegalStateException("EXECUTE requires barrier preflight");
    }
    if (mode == Mode.VERIFY && !status.equals("EXECUTED")) {
      throw new IllegalStateException("VERIFY requires completed execution");
    }
    if (mode == Mode.FINALIZE && !status.equals("VERIFIED")) {
      throw new IllegalStateException("FINALIZE requires VERIFIED");
    }
    if (mode == Mode.EXECUTE && mapper.countAnomalies(batchId) != 0) {
      throw new IllegalStateException("unresolved preflight anomalies");
    }
    return mode == Mode.EXECUTE ? mapper.selectPendingFamilies(batchId) : mapper.selectBatchFamilies(batchId);
  }

  /**
   * Executes one independently committed family migration.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   * @param familyId family identifier
   * @return {@code true} when the family is migrated or was already migrated
   */
  public boolean executeFamily(String token, Long batchId, Long epoch, Long familyId) {
    if (familyId == null) throw new IllegalArgumentException("family id required");
    return familyExecutor.execute(token, required(batchId), required(epoch), familyId);
  }

  /**
   * Marks execution complete only when no eligible family remains pending.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   */
  @Transactional
  public void markExecutionComplete(String token, Long batchId, Long epoch) {
    validateContinuation(token, batchId, epoch, Mode.EXECUTE);
    if (mapper.countPendingFamilies(batchId) != 0) {
      throw new IllegalStateException("eligible families remain pending");
    }
    if (mapper.markExecuted(batchId, epoch) != 1) throw new IllegalStateException("execution transition rejected");
  }

  /**
   * Verifies per-family and global wallet, hold, and selection invariants.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   * @return verified result
   */
  @Transactional
  public Result verify(String token, Long batchId, Long epoch) {
    List<Long> families = validateContinuation(token, batchId, epoch, Mode.VERIFY);
    if (mapper.countPendingFamilies(batchId) != 0 || mapper.countUnmigratedWallets(batchId) != 0
        || mapper.countMissingExpectedHolds(batchId) != 0 || families.isEmpty()) {
      throw new IllegalStateException("migration has pending or unmigrated eligible sources");
    }
    for (Long familyId : families) {
      if (mapper.countFamilyVerificationFailures(batchId, familyId) != 0) {
        throw new IllegalStateException("family conservation verification failed: " + familyId);
      }
    }
    Map<String, Object> totals = mapper.verificationTotals(batchId);
    BigDecimal sourceAvailable = decimal(totals.get("sourceAvailable"));
    BigDecimal sourceFrozen = decimal(totals.get("sourceFrozen"));
    BigDecimal targetAvailable = decimal(totals.get("targetAvailable"));
    BigDecimal targetFrozen = decimal(totals.get("targetFrozen"));
    if (sourceAvailable.compareTo(targetAvailable) != 0 || sourceFrozen.compareTo(targetFrozen) != 0
        || number(totals, "nonzeroSources", 0).longValue() != 0
        || number(totals, "badSelections", 0).longValue() != 0
        || mapper.countInvalidHolds(batchId) != 0 || mapper.countAnomalies(batchId) != 0) {
      throw new IllegalStateException("global migration conservation verification failed");
    }
    if (mapper.markVerified(batchId, epoch, sourceAvailable, sourceFrozen, targetAvailable, targetFrozen) != 1) {
      throw new IllegalStateException("verification transition rejected");
    }
    return new Result(batchId, epoch, "VERIFIED", 0);
  }

  /**
   * Applies resumable final cart DDL and publishes family-ready state.
   *
   * @param token runner owner token
   * @param batchId verified migration batch identifier
   * @param epoch verified drain epoch
   * @return finalized result
   */
  public Result finalizeBatch(String token, Long batchId, Long epoch) {
    validateContinuation(token, batchId, epoch, Mode.FINALIZE);
    ddl.dropLegacyIndex(token, batchId, epoch);
    ddl.dropLegacyColumn(token, batchId, epoch);
    ddl.addFamilyColumn(token, batchId, epoch);
    ddl.addFamilyIndex(token, batchId, epoch);
    ddl.cutover(token, batchId, epoch);
    return new Result(batchId, epoch, "FINALIZED", 0);
  }

  /**
   * Reads the database selected by the current connection for safety validation.
   *
   * @return current database name
   */
  @Transactional(readOnly = true)
  public String currentDatabase() { return mapper.currentDatabase(); }

  private Map<String, Object> requireBatch(Long batchId) {
    if (batchId == null) throw new IllegalArgumentException("batch id required");
    Map<String, Object> batch = mapper.lockBatch(batchId);
    if (batch == null) throw new IllegalStateException("unknown or stale batch id");
    return batch;
  }

  private void requireDrainProof(long batchId, long epoch) {
    Map<String, Object> cutover = mapper.lockCutover();
    if (cutover == null || !bool(cutover.get("maintenanceEnabled"))
        || !"DRAINED".equals(text(cutover.get("state")))
        || number(cutover, "activeBatchId", -1).longValue() != batchId
        || number(cutover, "drainEpoch", -1).longValue() != epoch || mapper.countActiveLeases() != 0) {
      throw new IllegalStateException("current unexpired drain proof is required");
    }
  }

  private static void requireEpoch(Map<String, Object> batch, Long epoch) {
    if (epoch == null || number(batch, "drainEpoch", -1).longValue() != epoch) {
      throw new IllegalStateException("stale drain epoch");
    }
  }
  private static void requireOneOf(Map<String, Object> batch, String first, String second) {
    String status = text(batch.get("status"));
    if (!first.equals(status) && !second.equals(status)) throw new IllegalStateException("illegal migration phase");
  }
  private static long required(Long value) {
    if (value == null) throw new IllegalArgumentException("batch and epoch are required");
    return value;
  }
  private static Number number(Map<String, Object> row, String key, Number fallback) {
    Object value = row.get(key);
    return value instanceof Number n ? n : fallback;
  }
  static BigDecimal decimal(Object value) {
    return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
  }
  static String text(Object value) { return value == null ? "" : value.toString(); }
  private static boolean bool(Object value) {
    return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
  }
}
