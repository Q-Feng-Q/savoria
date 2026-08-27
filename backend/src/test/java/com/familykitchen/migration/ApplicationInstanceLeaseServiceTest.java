package com.familykitchen.migration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies a live compatibility process cannot be mistaken for a drained process. */
class ApplicationInstanceLeaseServiceTest {
  @Test
  void continuesHeartbeatWhileMaintenanceBlocksBusinessWrites() {
    FamilyCartWalletMigrationMapper mapper = mock(FamilyCartWalletMigrationMapper.class);
    when(mapper.lockCutover()).thenReturn(Map.of("maintenanceEnabled", 1));
    ApplicationInstanceLeaseService service =
        new ApplicationInstanceLeaseService(mapper, "web-1", "compat-build");

    service.heartbeat();

    verify(mapper).ensureCutover();
    verify(mapper).lockCutover();
    verify(mapper).heartbeat("web-1", "compat-build", "web", 30);
  }
}
