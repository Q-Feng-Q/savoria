package com.familykitchen.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;

/** Registers datasource properties only after verifying the running test-owned container. */
public final class SafeTestDatabaseProperties {

  private SafeTestDatabaseProperties() { }

  /**
   * Registers datasource properties from a verified test-owned container.
   *
   * @param registry Spring dynamic property registry
   * @param container container owned by the current test JVM
   */
  public static void register(DynamicPropertyRegistry registry, MySQLContainer<?> container) {
    registry.add("spring.datasource.url", () -> checked(container, container.getJdbcUrl()));
    registry.add("spring.datasource.username", () -> checked(container, container.getUsername()));
    registry.add("spring.datasource.password", () -> checked(container, container.getPassword()));
    registry.add("spring.datasource.driver-class-name", () -> {
      check(container);
      return container.getDriverClassName();
    });
  }

  private static String checked(MySQLContainer<?> container, String value) {
    check(container);
    return value;
  }

  private static void check(MySQLContainer<?> container) {
    TestDatabaseUrlGuard.requireOwnedContainer(container, container.getJdbcUrl(),
        container.getUsername(), container.getPassword(), "test-container");
  }
}
