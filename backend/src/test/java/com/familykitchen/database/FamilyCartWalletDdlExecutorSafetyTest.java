package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.migration.FamilyCartWalletDdlExecutor;
import com.familykitchen.migration.FamilyCartWalletMigrationMapper;
import com.familykitchen.migration.FamilyWalletMigrationLeaseService;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit-level guards for irreversible finalization DDL. */
class FamilyCartWalletDdlExecutorSafetyTest {
  private final FamilyCartWalletMigrationMapper mapper = mock(FamilyCartWalletMigrationMapper.class);
  private final FamilyWalletMigrationLeaseService leases = mock(FamilyWalletMigrationLeaseService.class);
  private final FamilyCartWalletDdlExecutor ddl = new FamilyCartWalletDdlExecutor(mapper, leases);

  @Test
  void rejectsDrainProofOwnedByAnotherBatchBeforeInspectingDdlInventory() {
    when(mapper.lockBatch(9)).thenReturn(Map.of("status", "VERIFIED"));
    when(mapper.lockCutover()).thenReturn(Map.of(
        "maintenanceEnabled", 1, "state", "DRAINED", "activeBatchId", 10L, "drainEpoch", 7L));
    when(mapper.countActiveLeases()).thenReturn(0);

    assertThrows(IllegalStateException.class, () -> ddl.dropLegacyIndex("owner", 9, 7));

    verify(mapper, never()).indexExists("uk_carts_active_cart");
  }

  @Test
  void rejectsFamilyIndexThatExistsOnTheWrongColumn() {
    verifiedDrain();
    when(mapper.indexExists("uk_carts_active_cart")).thenReturn(0);
    when(mapper.columnExists("active_cart_key")).thenReturn(0);
    when(mapper.columnExists("active_family_id")).thenReturn(1);
    when(mapper.generatedColumnMetadata("active_family_id")).thenReturn(validColumn());
    when(mapper.exactUniqueIndexExists("uk_carts_active_family", "active_family_id")).thenReturn(0);
    when(mapper.setFamilyReady(9, 7)).thenReturn(1);
    when(mapper.markBatchFinalized(9)).thenReturn(1);

    assertThrows(IllegalStateException.class, () -> ddl.cutover("owner", 9, 7));

    verify(mapper, never()).setFamilyReady(9, 7);
  }

  @Test
  void rejectsGeneratedColumnWithOnlySuperficiallyMatchingTokens() {
    verifiedDrain();
    when(mapper.columnExists("active_family_id")).thenReturn(1);
    when(mapper.generatedColumnMetadata("active_family_id")).thenReturn(Map.of(
        "dataType", "bigint", "extra", "STORED GENERATED",
        "generationExpression", "case when status='inactive' then family_id else 0 end"));

    assertThrows(IllegalStateException.class, () -> ddl.addFamilyColumn("owner", 9, 7));
  }

  private void verifiedDrain() {
    when(mapper.lockBatch(9)).thenReturn(Map.of("status", "VERIFIED"));
    when(mapper.lockCutover()).thenReturn(Map.of(
        "maintenanceEnabled", 1, "state", "DRAINED", "activeBatchId", 9L, "drainEpoch", 7L));
    when(mapper.countActiveLeases()).thenReturn(0);
  }

  private static Map<String, Object> validColumn() {
    return Map.of("dataType", "bigint", "extra", "STORED GENERATED",
        "generationExpression", "case when (`status` = 'active') then `family_id` else NULL end");
  }
}
