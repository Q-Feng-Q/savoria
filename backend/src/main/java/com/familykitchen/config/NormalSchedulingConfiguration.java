package com.familykitchen.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables scheduled business jobs only during normal, non-migration application operation. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "family-kitchen.migration.mode", havingValue = "OFF",
    matchIfMissing = true)
public class NormalSchedulingConfiguration {
}
