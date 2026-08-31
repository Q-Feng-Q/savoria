package com.familykitchen.testsupport;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.testcontainers.containers.MySQLContainer;

/** Creates Flyway only for a running MySQL container owned by the current test JVM. */
public final class SafeTestFlyway {

  private SafeTestFlyway() { }

  /**
   * Creates a Flyway configuration bound to a verified test-owned container.
   *
   * @param container container owned by the current test JVM
   * @return guarded Flyway configuration
   */
  public static FluentConfiguration configure(MySQLContainer<?> container) {
    TestDatabaseUrlGuard.requireOwnedContainer(container, container.getJdbcUrl(),
        container.getUsername(), container.getPassword(), "test-container");
    return Flyway.configure().dataSource(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }
}
