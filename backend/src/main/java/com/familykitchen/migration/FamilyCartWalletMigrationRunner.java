package com.familykitchen.migration;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/** Explicitly gated runner protected by a persistent cross-process owner lease. */
@Component
public class FamilyCartWalletMigrationRunner implements ApplicationRunner, Ordered {
  /** Explicit migration lifecycle modes. */
  public enum Mode {
    /** Keeps the runner completely inert. */
    OFF,
    /** Creates or refreshes an anomaly report without moving data. */
    PREFLIGHT,
    /** Publishes the write barrier and waits for compatibility instances to drain. */
    QUIESCE,
    /** Removes the barrier before any family data or finalization DDL is written. */
    ABORT_BEFORE_EXECUTE,
    /** Migrates eligible families in independently committed transactions. */
    EXECUTE,
    /** Verifies per-family and global conservation after execution. */
    VERIFY,
    /** Applies resumable final DDL and publishes family-ready state. */
    FINALIZE
  }

  private final FamilyCartWalletMigrationService service;
  private final FamilyWalletMigrationLeaseService leases;
  private final Mode mode;
  private final Long batchId;
  private final Long drainEpoch;
  private final String expectedDatabase;
  private final String safetyToken;
  private final String configuredToken;
  private final ConfigurableApplicationContext context;

  /**
   * Creates the explicitly gated application runner.
   *
   * @param service migration orchestration service
   * @param leases persistent runner lease service
   * @param mode configured lifecycle mode
   * @param batchId optional migration batch identifier
   * @param drainEpoch optional drain epoch
   * @param expectedDatabase required disposable database name
   * @param safetyToken supplied safety token
   * @param configuredToken configured expected safety token
   * @param context application context closed after any one-shot migration command
   */
  public FamilyCartWalletMigrationRunner(FamilyCartWalletMigrationService service,
      FamilyWalletMigrationLeaseService leases,
      @Value("${family-kitchen.migration.mode:OFF}") String mode,
      @Value("${family-kitchen.migration.batch-id:#{null}}") Long batchId,
      @Value("${family-kitchen.migration.drain-epoch:#{null}}") Long drainEpoch,
      @Value("${family-kitchen.migration.expected-database:}") String expectedDatabase,
      @Value("${family-kitchen.migration.safety-token:}") String safetyToken,
      @Value("${family-kitchen.migration.configured-token:}") String configuredToken,
      ConfigurableApplicationContext context) {
    this.service = service;
    this.leases = leases;
    this.mode = Mode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
    this.batchId = batchId;
    this.drainEpoch = drainEpoch;
    this.expectedDatabase = expectedDatabase;
    this.safetyToken = safetyToken;
    this.configuredToken = configuredToken;
    this.context = context;
  }

  /** {@inheritDoc} */
  @Override
  public void run(ApplicationArguments args) {
    if (mode == Mode.OFF) return;
    if (context instanceof WebServerApplicationContext) {
      throw new IllegalStateException("migration mode must run as a non-web one-shot process");
    }
    validateArguments();
    validateSafety();
    long leaseBatch = batchId == null ? 0 : batchId;
    long leaseEpoch = drainEpoch == null ? 0 : drainEpoch;
    String token = leases.acquire(leaseBatch, leaseEpoch, mode.name());
    try {
      runOwned(token, leaseBatch, leaseEpoch);
    } finally {
      try {
        leases.release(token);
      } finally {
        context.close();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  private void runOwned(String token, long leaseBatch, long leaseEpoch) {
    FamilyCartWalletMigrationService.Result result;
    switch (mode) {
      case PREFLIGHT -> result = service.preflight(token, batchId, drainEpoch);
      case QUIESCE -> result = service.quiesce(token, batchId, "migration-runner");
      case ABORT_BEFORE_EXECUTE -> result = service.abortBeforeExecute(token, batchId, drainEpoch);
      case EXECUTE -> {
        List<Long> families = service.validateContinuation(token, batchId, drainEpoch, mode);
        for (Long familyId : families) {
          leases.renew(token, leaseBatch, leaseEpoch);
          if (!service.executeFamily(token, batchId, drainEpoch, familyId)) {
            throw new IllegalStateException("family migration failed: " + familyId);
          }
        }
        leases.renew(token, leaseBatch, leaseEpoch);
        service.markExecutionComplete(token, batchId, drainEpoch);
        result = new FamilyCartWalletMigrationService.Result(batchId, drainEpoch, "EXECUTED", 0);
      }
      case VERIFY -> result = service.verify(token, batchId, drainEpoch);
      case FINALIZE -> result = service.finalizeBatch(token, batchId, drainEpoch);
      default -> throw new IllegalStateException("unsupported mode " + mode);
    }
    System.out.printf("family migration mode=%s batchId=%d drainEpoch=%d status=%s anomalies=%d%n",
        mode, result.batchId(), result.drainEpoch(), result.status(), result.anomalyCount());
  }

  private void validateArguments() {
    if (mode != Mode.PREFLIGHT && batchId == null) {
      throw new IllegalArgumentException(mode + " requires batch-id");
    }
    if (mode == Mode.PREFLIGHT && drainEpoch != null && batchId == null) {
      throw new IllegalArgumentException("barrier PREFLIGHT requires batch-id");
    }
    if (mode == Mode.QUIESCE && drainEpoch != null) {
      throw new IllegalArgumentException("QUIESCE allocates drain-epoch and does not accept one");
    }
    if (requiresEpoch(mode) && drainEpoch == null) {
      throw new IllegalArgumentException(mode + " requires drain-epoch");
    }
    if (batchId != null && batchId <= 0) {
      throw new IllegalArgumentException("batch-id must be positive");
    }
    if (drainEpoch != null && drainEpoch <= 0) {
      throw new IllegalArgumentException("drain-epoch must be positive");
    }
  }

  private void validateSafety() {
    String database = expectedDatabase == null ? "" : expectedDatabase.trim().toLowerCase(Locale.ROOT);
    if (database.isEmpty() || database.equals("family_kitchen") || database.contains("prod")
        || !database.endsWith("_family_wallet_disposable")) {
      throw new IllegalStateException("migration modes require an explicit database ending _family_wallet_disposable");
    }
    if (configuredToken == null || configuredToken.isBlank()
        || !Objects.equals(configuredToken, safetyToken)) {
      throw new IllegalStateException("migration safety token rejected");
    }
    String actual = service.currentDatabase();
    if (actual == null || actual.isBlank()
        || !database.equals(actual.trim().toLowerCase(Locale.ROOT))) {
      throw new IllegalStateException("migration database target mismatch");
    }
  }

  private static boolean requiresEpoch(Mode value) {
    return value == Mode.ABORT_BEFORE_EXECUTE || value == Mode.EXECUTE
        || value == Mode.VERIFY || value == Mode.FINALIZE;
  }
}
