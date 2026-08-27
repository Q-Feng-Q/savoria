package com.familykitchen.migration;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Wraps every non-migration business transaction in the persistent cutover-row fence.
 * This covers HTTP services, scheduled jobs, startup initializers, and other non-HTTP writers.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "family-kitchen.instance.lease-enabled", havingValue = "true")
public class FamilyWalletBusinessTransactionBarrierAspect {
  private final FamilyCartWalletMigrationMapper mapper;
  private final TransactionTemplate transactions;

  /**
   * Creates the transaction-level compatibility write barrier.
   *
   * @param mapper migration persistence mapper
   * @param transactionManager application transaction manager
   */
  public FamilyWalletBusinessTransactionBarrierAspect(FamilyCartWalletMigrationMapper mapper,
      PlatformTransactionManager transactionManager) {
    this.mapper = mapper;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  @Around("execution(* com.familykitchen..*(..))"
      + " && @annotation(transactional)"
      + " && !within(com.familykitchen.migration..*)")
  Object guardMethodTransaction(ProceedingJoinPoint joinPoint, Transactional transactional)
      throws Throwable {
    return transactional.readOnly() ? joinPoint.proceed() : guarded(joinPoint);
  }

  @Around("execution(public * com.familykitchen..*(..))"
      + " && @within(transactional)"
      + " && !@annotation(org.springframework.transaction.annotation.Transactional)"
      + " && !within(com.familykitchen.migration..*)")
  Object guardClassTransaction(ProceedingJoinPoint joinPoint, Transactional transactional)
      throws Throwable {
    return transactional.readOnly() ? joinPoint.proceed() : guarded(joinPoint);
  }

  private Object guarded(ProceedingJoinPoint joinPoint) {
    return transactions.execute(status -> {
      mapper.ensureCutover();
      Map<String, Object> cutover = mapper.lockCutover();
      if (cutover != null && truth(cutover.get("maintenanceEnabled"))) {
        throw new IllegalStateException("family wallet migration write barrier is active");
      }
      try {
        return joinPoint.proceed();
      } catch (RuntimeException | Error exception) {
        throw exception;
      } catch (Throwable throwable) {
        throw new IllegalStateException("business transaction failed", throwable);
      }
    });
  }

  private static boolean truth(Object value) {
    return value instanceof Boolean flag ? flag : value instanceof Number number && number.intValue() != 0;
  }
}
