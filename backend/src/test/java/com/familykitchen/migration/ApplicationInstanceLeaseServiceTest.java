package com.familykitchen.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Transactional;

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

  @Test
  void startupRegistrationIsAnEarlyTransactionalRunner() throws Exception {
    FamilyCartWalletMigrationMapper mapper = mock(FamilyCartWalletMigrationMapper.class);
    ApplicationInstanceLeaseService service =
        new ApplicationInstanceLeaseService(mapper, "web-1", "compat-build");

    service.run(new DefaultApplicationArguments());

    verify(mapper).ensureCutover();
    verify(mapper).lockCutover();
    verify(mapper).heartbeat("web-1", "compat-build", "web", 30);
    assertTrue(ApplicationInstanceLeaseService.class
        .getMethod("run", org.springframework.boot.ApplicationArguments.class)
        .isAnnotationPresent(Transactional.class));
    assertTrue(service.getOrder() > Ordered.HIGHEST_PRECEDENCE);
    assertTrue(service.getOrder() < Ordered.LOWEST_PRECEDENCE);
  }
}
