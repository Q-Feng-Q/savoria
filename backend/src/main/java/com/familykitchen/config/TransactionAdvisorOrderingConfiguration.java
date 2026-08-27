package com.familykitchen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Starts the declared business transaction before the family-wallet cutover fence runs. */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class TransactionAdvisorOrderingConfiguration {
}
