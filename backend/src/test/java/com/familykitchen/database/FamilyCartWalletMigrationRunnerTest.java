package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.migration.FamilyCartWalletMigrationRunner;
import com.familykitchen.migration.FamilyCartWalletMigrationRunner.Mode;
import com.familykitchen.migration.FamilyCartWalletMigrationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/** Verifies migration-runner dispatch and safety gates without a database. */
class FamilyCartWalletMigrationRunnerTest {

  private final FamilyCartWalletMigrationService service = mock(FamilyCartWalletMigrationService.class);

  @Test
  void defaultOffDoesNotReadOrWriteMigrationState() {
    runner("OFF", null, null, "", "", "").run(new DefaultApplicationArguments());
    verify(service, never()).preflight(null, null);
    verify(service, never()).quiesce(null, null);
  }

  @Test
  void initialPreflightCreatesAReportOnlyWithDisposableTargetAndSafetyToken() {
    when(service.preflight(null, null)).thenReturn(
        new FamilyCartWalletMigrationService.Result(41L, 0L, "PREFLIGHT", 0));
    assertDoesNotThrow(() -> runner("PREFLIGHT", null, null,
        "wallet_family_wallet_disposable", "once", "once")
        .run(new DefaultApplicationArguments()));
    verify(service).preflight(null, null);
  }

  @Test
  void everyContinuationRequiresBatchAndQuiescedModesRequireEpoch() {
    assertThrows(IllegalArgumentException.class,
        () -> runner("QUIESCE", null, null, "db_family_wallet_disposable", "once", "once").run(args()));
    assertThrows(IllegalArgumentException.class,
        () -> runner("EXECUTE", 9L, null, "db_family_wallet_disposable", "once", "once").run(args()));
    assertThrows(IllegalArgumentException.class,
        () -> runner("VERIFY", 9L, null, "db_family_wallet_disposable", "once", "once").run(args()));
    assertThrows(IllegalArgumentException.class,
        () -> runner("FINALIZE", 9L, null, "db_family_wallet_disposable", "once", "once").run(args()));
  }

  @Test
  void mutatingModesRejectMissingMismatchedOrProductionLikeSafetyConfiguration() {
    assertThrows(IllegalStateException.class,
        () -> runner("QUIESCE", 9L, null, "", "once", "once").run(args()));
    assertThrows(IllegalStateException.class,
        () -> runner("QUIESCE", 9L, null, "family_kitchen_prod", "once", "once").run(args()));
    assertThrows(IllegalStateException.class,
        () -> runner("QUIESCE", 9L, null, "migration_test", "bad", "once").run(args()));
    assertThrows(IllegalStateException.class,
        () -> runner("PREFLIGHT", null, null, "migration_test", "once", "once").run(args()));
  }

  @Test
  void executeProcessesFamiliesThroughSeparateServiceCallsAndResumes() {
    when(service.validateContinuation(12L, 7L, Mode.EXECUTE)).thenReturn(List.of(101L, 102L));
    when(service.executeFamily(12L, 7L, 101L)).thenReturn(true);
    when(service.executeFamily(12L, 7L, 102L)).thenReturn(false);

    assertThrows(IllegalStateException.class,
        () -> runner("EXECUTE", 12L, 7L,
            "runner_family_wallet_disposable", "once", "once").run(args()));

    verify(service).executeFamily(12L, 7L, 101L);
    verify(service).executeFamily(12L, 7L, 102L);
    verify(service, never()).markExecutionComplete(12L, 7L);
  }

  @Test
  void dispatchesEveryNonExecuteContinuationMode() {
    when(service.quiesce(9L, "migration-runner")).thenReturn(
        new FamilyCartWalletMigrationService.Result(9L, 4L, "DRAINED", 0));
    when(service.abortBeforeExecute(9L, 4L)).thenReturn(
        new FamilyCartWalletMigrationService.Result(9L, 4L, "ABORTED", 0));
    when(service.verify(9L, 4L)).thenReturn(
        new FamilyCartWalletMigrationService.Result(9L, 4L, "VERIFIED", 0));
    when(service.finalizeBatch(9L, 4L)).thenReturn(
        new FamilyCartWalletMigrationService.Result(9L, 4L, "FINALIZED", 0));

    runner("QUIESCE", 9L, null, "wallet_family_wallet_disposable", "once", "once").run(args());
    runner("ABORT_BEFORE_EXECUTE", 9L, 4L, "wallet_family_wallet_disposable", "once", "once").run(args());
    runner("VERIFY", 9L, 4L, "wallet_family_wallet_disposable", "once", "once").run(args());
    runner("FINALIZE", 9L, 4L, "wallet_family_wallet_disposable", "once", "once").run(args());

    verify(service).quiesce(9L, "migration-runner");
    verify(service).abortBeforeExecute(9L, 4L);
    verify(service).verify(9L, 4L);
    verify(service).finalizeBatch(9L, 4L);
  }

  @Test
  void mutatingModeRejectsActualDatabaseMismatchBeforeWriting() {
    when(service.currentDatabase()).thenReturn("other_family_wallet_disposable");
    assertThrows(IllegalStateException.class,
        () -> runner("QUIESCE", 9L, null,
            "wallet_family_wallet_disposable", "once", "once").run(args()));
    verify(service, never()).quiesce(9L, "migration-runner");
  }

  private FamilyCartWalletMigrationRunner runner(
      String mode, Long batchId, Long epoch, String database, String token, String configuredToken) {
    return new FamilyCartWalletMigrationRunner(
        service, mode, batchId, epoch, database, token, configuredToken);
  }

  private static DefaultApplicationArguments args() {
    return new DefaultApplicationArguments();
  }
}
