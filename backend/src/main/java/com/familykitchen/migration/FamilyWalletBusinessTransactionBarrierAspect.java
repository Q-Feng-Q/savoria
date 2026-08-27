package com.familykitchen.migration;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wraps every non-migration business transaction in the persistent cutover-row fence.
 * This covers HTTP services, scheduled jobs, startup initializers, and other non-HTTP writers.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnExpression("${family-kitchen.instance.lease-enabled:false}"
    + " && '${family-kitchen.migration.mode:OFF}'.equalsIgnoreCase('OFF')")
public class FamilyWalletBusinessTransactionBarrierAspect {
  private final FamilyCartWalletMigrationMapper mapper;

  /**
   * Creates the transaction-level compatibility write barrier.
   *
   * @param mapper migration persistence mapper
   */
  public FamilyWalletBusinessTransactionBarrierAspect(FamilyCartWalletMigrationMapper mapper) {
    this.mapper = mapper;
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

  private Object guarded(ProceedingJoinPoint joinPoint) throws Throwable {
    mapper.ensureCutover();
    Map<String, Object> cutover = mapper.lockCutover();
    if (cutover != null && (truth(cutover.get("maintenanceEnabled"))
        || "FAMILY_READY".equals(String.valueOf(cutover.get("state"))))) {
      throw new IllegalStateException("family wallet migration write barrier is active");
    }
    return joinPoint.proceed();
  }

  private static boolean truth(Object value) {
    return value instanceof Boolean flag ? flag : value instanceof Number number && number.intValue() != 0;
  }
}
