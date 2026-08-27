package com.familykitchen.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Verifies the persistent transaction fence also covers non-HTTP business writers. */
class FamilyWalletBusinessTransactionBarrierAspectTest {
  @Test
  void blocksScheduledOrHttpWriterInsideTheSameDatabaseTransaction() throws Throwable {
    FamilyCartWalletMigrationMapper mapper = mock(FamilyCartWalletMigrationMapper.class);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Transactional transactional = mock(Transactional.class);
    when(mapper.lockCutover()).thenReturn(Map.of("maintenanceEnabled", 1));
    FamilyWalletBusinessTransactionBarrierAspect aspect =
        new FamilyWalletBusinessTransactionBarrierAspect(mapper);

    assertThrows(IllegalStateException.class,
        () -> aspect.guardMethodTransaction(joinPoint, transactional));

    verify(joinPoint, never()).proceed();
  }

  @Test
  void explicitReadOnlyTransactionRemainsAvailable() throws Throwable {
    FamilyCartWalletMigrationMapper mapper = mock(FamilyCartWalletMigrationMapper.class);
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Transactional transactional = mock(Transactional.class);
    when(transactional.readOnly()).thenReturn(true);
    when(joinPoint.proceed()).thenReturn("read-result");
    FamilyWalletBusinessTransactionBarrierAspect aspect =
        new FamilyWalletBusinessTransactionBarrierAspect(mapper);

    assertEquals("read-result", aspect.guardMethodTransaction(joinPoint, transactional));

    verify(mapper, never()).lockCutover();
  }
}
