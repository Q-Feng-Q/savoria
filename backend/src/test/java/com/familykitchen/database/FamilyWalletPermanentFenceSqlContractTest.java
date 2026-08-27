package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Prevents a paused compatibility process from resuming writes after final cutover. */
class FamilyWalletPermanentFenceSqlContractTest {
  @Test
  void familyReadyStateRemainsAWriteBarrierForCompatibilityBuilds() throws Exception {
    String sql;
    try (var stream = getClass().getResourceAsStream(
        "/mapper/migration/FamilyCartWalletMigrationMapper.xml")) {
      if (stream == null) throw new IllegalStateException("migration mapper XML is missing");
      sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
          .replaceAll("\\s+", " ").toLowerCase();
    }
    assertTrue(sql.contains("maintenance_enabled=1 or state='family_ready'"));
  }
}
