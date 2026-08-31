package com.familykitchen.testsupport;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Installs the final datasource guard in every Spring Boot test context. */
@AutoConfiguration
public class TestDatabaseGuardAutoConfiguration {

  /**
   * Creates the guard that validates properties after dynamic test overrides are merged.
   *
   * @return final datasource property guard
   */
  @Bean
  static TestDatabaseFinalPropertyGuard testDatabaseFinalPropertyGuard() {
    return new TestDatabaseFinalPropertyGuard();
  }
}
