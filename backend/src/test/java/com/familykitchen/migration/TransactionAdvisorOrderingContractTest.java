package com.familykitchen.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.config.TransactionAdvisorOrderingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Keeps the declared transaction outside the migration fence so its attributes remain authoritative. */
class TransactionAdvisorOrderingContractTest {
  @Test
  void transactionAdvisorRunsBeforeBarrierAdvisor() {
    EnableTransactionManagement transactions =
        TransactionAdvisorOrderingConfiguration.class.getAnnotation(EnableTransactionManagement.class);
    Order barrier = FamilyWalletBusinessTransactionBarrierAspect.class.getAnnotation(Order.class);

    assertEquals(Ordered.HIGHEST_PRECEDENCE, transactions.order());
    assertTrue(transactions.order() < barrier.value());
  }
}
