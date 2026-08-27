package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.familykitchen.migration.FamilyCartWalletMigrationRunner;
import com.familykitchen.migration.FamilyCartWalletMigrationRunner.Mode;
import com.familykitchen.migration.FamilyCartWalletMigrationService;
import com.familykitchen.migration.FamilyWalletMigrationLeaseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;

/** Verifies persistent lease ownership and dispatch without a database. */
class FamilyCartWalletMigrationRunnerTest {
  private final FamilyCartWalletMigrationService service = mock(FamilyCartWalletMigrationService.class);
  private final FamilyWalletMigrationLeaseService leases = mock(FamilyWalletMigrationLeaseService.class);
  private final ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

  @Test
  void defaultOffDoesNotTouchServiceOrLease() {
    runner("OFF", null, null, "", "", "").run(args());
    verify(leases, never()).acquire(0, 0, "OFF");
    verify(service, never()).currentDatabase();
    verify(context, never()).close();
  }

  @Test
  void preflightIsPersistentlyOwnedAndAlwaysReleased() {
    when(service.currentDatabase()).thenReturn("wallet_family_wallet_disposable");
    when(leases.acquire(0, 0, "PREFLIGHT")).thenReturn("owner");
    when(service.preflight("owner", null, null)).thenReturn(
        new FamilyCartWalletMigrationService.Result(41, 0, "PREFLIGHT", 0));
    assertDoesNotThrow(() -> runner("PREFLIGHT", null, null,
        "wallet_family_wallet_disposable", "once", "once").run(args()));
    verify(service).preflight("owner", null, null);
    verify(leases).release("owner");
    verify(context).close();
  }

  @Test
  void everyContinuationRequiresBatchAndQuiescedModesRequireEpochBeforeLease() {
    assertThrows(IllegalArgumentException.class, () -> runner("QUIESCE", null, null,
        "db_family_wallet_disposable", "once", "once").run(args()));
    assertThrows(IllegalArgumentException.class, () -> runner("EXECUTE", 9L, null,
        "db_family_wallet_disposable", "once", "once").run(args()));
    verify(leases, never()).acquire(0, 0, "QUIESCE");
  }

  @Test
  void barrierPreflightEpochCannotBeSuppliedWithoutItsBatch() {
    assertThrows(IllegalArgumentException.class, () -> runner("PREFLIGHT", null, 7L,
        "db_family_wallet_disposable", "once", "once").run(args()));
    verify(leases, never()).acquire(0, 7, "PREFLIGHT");
  }

  @Test
  void quiesceAllocatesItsEpochAndRejectsAConfiguredOne() {
    assertThrows(IllegalArgumentException.class, () -> runner("QUIESCE", 9L, 7L,
        "db_family_wallet_disposable", "once", "once").run(args()));
    verify(leases, never()).acquire(9, 7, "QUIESCE");
  }

  @Test
  void safetyValidationPrecedesAnyPersistentWrite() {
    assertThrows(IllegalStateException.class, () -> runner("QUIESCE", 9L, null,
        "family_kitchen_prod", "once", "once").run(args()));
    assertThrows(IllegalStateException.class, () -> runner("PREFLIGHT", null, null,
        "migration_test", "bad", "once").run(args()));
    verify(leases, never()).acquire(9, 0, "QUIESCE");
  }

  @Test
  void executeRenewsBetweenIndependentFamiliesAndDoesNotMarkPartialRunComplete() {
    when(service.currentDatabase()).thenReturn("runner_family_wallet_disposable");
    when(leases.acquire(12, 7, "EXECUTE")).thenReturn("owner");
    when(service.validateContinuation("owner", 12L, 7L, Mode.EXECUTE)).thenReturn(List.of(101L, 102L));
    when(service.executeFamily("owner", 12L, 7L, 101L)).thenReturn(true);
    when(service.executeFamily("owner", 12L, 7L, 102L)).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> runner("EXECUTE", 12L, 7L,
        "runner_family_wallet_disposable", "once", "once").run(args()));
    verify(leases, times(2)).renew("owner", 12, 7);
    verify(service, never()).markExecutionComplete("owner", 12L, 7L);
    verify(leases).release("owner");
    verify(context).close();
  }

  @Test
  void staleOwnerFailureStillReleasesOnlyItsToken() {
    when(service.currentDatabase()).thenReturn("wallet_family_wallet_disposable");
    when(leases.acquire(9, 4, "VERIFY")).thenReturn("owner");
    when(service.verify("owner", 9L, 4L)).thenThrow(new IllegalStateException("stale"));
    assertThrows(IllegalStateException.class, () -> runner("VERIFY", 9L, 4L,
        "wallet_family_wallet_disposable", "once", "once").run(args()));
    verify(leases).release("owner");
  }

  @Test
  void rejectsNonPositiveIdentifiersAndUnknownActualDatabaseBeforeLease() {
    assertThrows(IllegalArgumentException.class, () -> runner("EXECUTE", 0L, 7L,
        "runner_family_wallet_disposable", "once", "once").run(args()));
    assertThrows(IllegalArgumentException.class, () -> runner("EXECUTE", 9L, -1L,
        "runner_family_wallet_disposable", "once", "once").run(args()));
    when(service.currentDatabase()).thenReturn(null);
    assertThrows(IllegalStateException.class, () -> runner("PREFLIGHT", null, null,
        "runner_family_wallet_disposable", "once", "once").run(args()));
    verify(leases, never()).acquire(0, 0, "PREFLIGHT");
  }

  @Test
  void nonOffModeRejectsAWebApplicationContextBeforeDatabaseAccess() {
    ConfigurableWebServerApplicationContext webContext =
        mock(ConfigurableWebServerApplicationContext.class);
    FamilyCartWalletMigrationRunner webRunner = new FamilyCartWalletMigrationRunner(
        service, leases, "PREFLIGHT", null, null, "runner_family_wallet_disposable",
        "once", "once", webContext);

    assertThrows(IllegalStateException.class, () -> webRunner.run(args()));

    verify(service, never()).currentDatabase();
    verify(leases, never()).acquire(0, 0, "PREFLIGHT");
  }

  private FamilyCartWalletMigrationRunner runner(String mode, Long batch, Long epoch,
      String database, String token, String configured) {
    return new FamilyCartWalletMigrationRunner(service, leases, mode, batch, epoch,
        database, token, configured, context);
  }
  private static DefaultApplicationArguments args() { return new DefaultApplicationArguments(); }
}
