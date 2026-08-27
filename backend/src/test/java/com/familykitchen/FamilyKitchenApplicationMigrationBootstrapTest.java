package com.familykitchen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Verifies command-line migration mode is detected before creating a web application context. */
class FamilyKitchenApplicationMigrationBootstrapTest {
  @Test
  void explicitMigrationModeForcesOneShotBootstrap() {
    assertTrue(FamilyKitchenApplication.isMigrationCommand(
        new String[] {"--family-kitchen.migration.mode=PREFLIGHT"}));
    assertFalse(FamilyKitchenApplication.isMigrationCommand(
        new String[] {"--family-kitchen.migration.mode=OFF"}));
    assertFalse(FamilyKitchenApplication.isMigrationCommand(new String[0]));
  }
}
