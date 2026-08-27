package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.migration.FamilyCartWalletDdlExecutor;
import com.familykitchen.migration.FamilyCartWalletFamilyExecutor;
import com.familykitchen.migration.FamilyCartWalletMigrationMapper;
import com.familykitchen.migration.FamilyCartWalletMigrationRunner.Mode;
import com.familykitchen.migration.FamilyCartWalletMigrationService;
import com.familykitchen.migration.FamilyWalletMigrationBarrierService;
import com.familykitchen.migration.FamilyWalletMigrationLeaseService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Unit-level phase, fencing, and verification guards. */
class FamilyCartWalletMigrationServiceSafetyTest {
  private final FamilyCartWalletMigrationMapper mapper = mock(FamilyCartWalletMigrationMapper.class);
  private final FamilyCartWalletFamilyExecutor executor = mock(FamilyCartWalletFamilyExecutor.class);
  private final FamilyWalletMigrationLeaseService leases = mock(FamilyWalletMigrationLeaseService.class);
  private final FamilyWalletMigrationBarrierService barriers = mock(FamilyWalletMigrationBarrierService.class);
  private final FamilyCartWalletDdlExecutor ddl = mock(FamilyCartWalletDdlExecutor.class);
  private final FamilyCartWalletMigrationService service =
      new FamilyCartWalletMigrationService(mapper, executor, leases, barriers, ddl);

  @Test void verifyRejectsPendingFamilyBeforeReadingZeroTotals() {
    drainedBatch("EXECUTED");
    when(mapper.selectBatchFamilies(9)).thenReturn(List.of(101L));
    when(mapper.countPendingFamilies(9)).thenReturn(1);
    assertThrows(IllegalStateException.class, () -> service.verify("owner", 9L, 7L));
    verify(mapper, never()).verificationTotals(9);
  }

  @Test void executePhaseRejectsDrainedBatchUntilBarrierPreflightCommits() {
    drainedBatch("DRAINED");
    assertThrows(IllegalStateException.class,
        () -> service.validateContinuation("owner", 9L, 7L, Mode.EXECUTE));
    verify(mapper, never()).selectPendingFamilies(9);
  }

  @Test void finalizeRunsExactDdlStepsInOrderUnderSameOwnerProof() {
    drainedBatch("VERIFIED");
    when(mapper.selectBatchFamilies(9)).thenReturn(List.of(101L));
    service.finalizeBatch("owner", 9L, 7L);
    InOrder order = inOrder(ddl);
    order.verify(ddl).dropLegacyIndex("owner", 9, 7);
    order.verify(ddl).dropLegacyColumn("owner", 9, 7);
    order.verify(ddl).addFamilyColumn("owner", 9, 7);
    order.verify(ddl).addFamilyIndex("owner", 9, 7);
    order.verify(ddl).cutover("owner", 9, 7);
  }

  @Test void familyExecutorRejectsStaleOwnerBeforeAnySourceRead() {
    FamilyCartWalletFamilyExecutor real = new FamilyCartWalletFamilyExecutor(mapper, leases);
    org.mockito.Mockito.doThrow(new IllegalStateException("stale"))
        .when(leases).requireOwned("wrong", 9, 7);
    assertThrows(IllegalStateException.class, () -> real.execute("wrong", 9, 7, 101));
    verify(mapper, never()).selectWalletSources(101);
  }

  @Test void quiesceReportsCommittedBarrierPhaseResult() {
    when(barriers.enable("owner", 9, "runner")).thenReturn(7L);
    when(barriers.proveDrained("owner", 9, 7)).thenReturn(false);
    assertEquals("QUIESCING", service.quiesce("owner", 9L, "runner").status());
  }

  private void drainedBatch(String status) {
    when(mapper.lockBatch(9)).thenReturn(Map.of("status", status, "drainEpoch", 7L));
    when(mapper.lockCutover()).thenReturn(Map.of(
        "state", "DRAINED", "maintenanceEnabled", 1, "activeBatchId", 9L, "drainEpoch", 7L));
    when(mapper.countActiveLeases()).thenReturn(0);
  }
}