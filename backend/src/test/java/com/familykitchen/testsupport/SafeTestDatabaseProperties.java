package com.familykitchen.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;

/** Registers datasource properties only after verifying the running test-owned container. */
public final class SafeTestDatabaseProperties {

  private SafeTestDatabaseProperties() { }

  /**
   * Registers datasource properties from a verified test-owned container.
   *
   * @param registry Spring dynamic property registry
   * @param ownership registration for a container owned by the current test JVM
   */
  public static void register(DynamicPropertyRegistry registry,
      TestDatabaseOwnership.Registration ownership) {
    registry.add("spring.datasource.url", () -> checked(ownership,
        TestDatabaseOwnership.require(ownership).getJdbcUrl()));
    registry.add("spring.datasource.username", () -> checked(ownership,
        TestDatabaseOwnership.require(ownership).getUsername()));
    registry.add("spring.datasource.password", () -> checked(ownership,
        TestDatabaseOwnership.require(ownership).getPassword()));
    registry.add("spring.datasource.driver-class-name", () -> {
      check(ownership);
      return TestDatabaseOwnership.require(ownership).getDriverClassName();
    });
  }

  private static String checked(TestDatabaseOwnership.Registration ownership, String value) {
    check(ownership);
    return value;
  }

  private static void check(TestDatabaseOwnership.Registration ownership) {
    var container = TestDatabaseOwnership.require(ownership);
    TestDatabaseUrlGuard.requireOwnedContainer(ownership, container.getJdbcUrl(),
        container.getUsername(), container.getPassword(), "test-container");
  }
}
