package com.familykitchen.migration;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Explicitly gated, resumable command runner for the family cart/wallet cutover. */
@Component
public class FamilyCartWalletMigrationRunner implements ApplicationRunner {
  private static final AtomicBoolean RUNNING = new AtomicBoolean();

  /** Supported explicit migration phases. */
  public enum Mode {
    /** Performs no migration work. */
    OFF,
    /** Builds or refreshes the anomaly report. */
    PREFLIGHT,
    /** Enables the write barrier and proves instance drain. */
    QUIESCE,
    /** Clears the barrier before any source migration. */
    ABORT_BEFORE_EXECUTE,
    /** Migrates each family in an independent transaction. */
    EXECUTE,
    /** Verifies money and cart conservation. */
    VERIFY,
    /** Installs final DDL and enables family mode. */
    FINALIZE
  }

  private final FamilyCartWalletMigrationService service;
  private final Mode mode;
  private final Long batchId;
  private final Long drainEpoch;
  private final String expectedDatabase;
  private final String safetyToken;
  private final String configuredToken;

  /**
   * Creates the property-gated migration runner.
   *
   * @param service migration service
   * @param mode requested phase
   * @param batchId optional batch identifier
   * @param drainEpoch optional drain epoch
   * @param expectedDatabase explicitly approved disposable database
   * @param safetyToken operator-supplied token
   * @param configuredToken independently configured expected token
   */
  public FamilyCartWalletMigrationRunner(
      FamilyCartWalletMigrationService service,
      @Value("${family-kitchen.migration.mode:OFF}") String mode,
      @Value("${family-kitchen.migration.batch-id:#{null}}") Long batchId,
      @Value("${family-kitchen.migration.drain-epoch:#{null}}") Long drainEpoch,
      @Value("${family-kitchen.migration.expected-database:}") String expectedDatabase,
      @Value("${family-kitchen.migration.safety-token:}") String safetyToken,
      @Value("${family-kitchen.migration.configured-token:}") String configuredToken) {
    this.service = service;
    this.mode = Mode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
    this.batchId = batchId;
    this.drainEpoch = drainEpoch;
    this.expectedDatabase = expectedDatabase;
    this.safetyToken = safetyToken;
    this.configuredToken = configuredToken;
  }

  /** {@inheritDoc} */
  @Override
  public void run(ApplicationArguments args) {
    if (mode == Mode.OFF) return;
    if (!RUNNING.compareAndSet(false, true)) {
      throw new IllegalStateException("another family migration runner is active");
    }
    try {
      runExclusive();
    } finally {
      RUNNING.set(false);
    }
  }

  private void runExclusive() {
    if (mode != Mode.PREFLIGHT && batchId == null) {
      throw new IllegalArgumentException(mode + " requires batch-id");
    }
    if (requiresEpoch(mode) && drainEpoch == null) {
      throw new IllegalArgumentException(mode + " requires drain-epoch");
    }
    validateSafety();

    FamilyCartWalletMigrationService.Result result;
    switch (mode) {
      case PREFLIGHT -> result = service.preflight(batchId, drainEpoch);
      case QUIESCE -> result = service.quiesce(batchId, "migration-runner");
      case ABORT_BEFORE_EXECUTE -> result = service.abortBeforeExecute(batchId, drainEpoch);
      case EXECUTE -> {
        List<Long> families = service.validateContinuation(batchId, drainEpoch, mode);
        for (Long familyId : families) {
          if (!service.executeFamily(batchId, drainEpoch, familyId)) {
            throw new IllegalStateException("family migration failed: " + familyId);
          }
        }
        service.markExecutionComplete(batchId, drainEpoch);
        result = new FamilyCartWalletMigrationService.Result(batchId, drainEpoch, "EXECUTED", 0);
      }
      case VERIFY -> result = service.verify(batchId, drainEpoch);
      case FINALIZE -> result = service.finalizeBatch(batchId, drainEpoch);
      default -> throw new IllegalStateException("unsupported mode " + mode);
    }
    System.out.printf("family migration mode=%s batchId=%d drainEpoch=%d status=%s anomalies=%d%n",
        mode, result.batchId(), result.drainEpoch(), result.status(), result.anomalyCount());
  }

  private void validateSafety() {
    String database = expectedDatabase == null ? "" : expectedDatabase.trim().toLowerCase(Locale.ROOT);
    if (database.isEmpty() || database.equals("family_kitchen") || database.contains("prod")
        || !database.endsWith("_family_wallet_disposable")) {
      throw new IllegalStateException(
          "migration modes require an explicit database ending _family_wallet_disposable");
    }
    if (configuredToken == null || configuredToken.isBlank()
        || !Objects.equals(configuredToken, safetyToken)) {
      throw new IllegalStateException("migration safety token rejected");
    }
    String actual = service.currentDatabase();
    if (actual != null && !database.equals(actual.trim().toLowerCase(Locale.ROOT))) {
      throw new IllegalStateException("migration database target mismatch");
    }
  }

  private static boolean requiresEpoch(Mode value) {
    return value == Mode.ABORT_BEFORE_EXECUTE || value == Mode.EXECUTE
        || value == Mode.VERIFY || value == Mode.FINALIZE;
  }
}
