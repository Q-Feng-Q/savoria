package com.familykitchen.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.testcontainers.containers.MySQLContainer;

/** Opens direct JDBC connections only for a container owned by this test JVM. */
public final class SafeTestJdbc {

  private SafeTestJdbc() { }

  /**
   * Opens a guarded connection to the exact registered container.
   *
   * @param ownership opaque current-JVM container registration
   * @return JDBC connection to the registered container
   * @throws SQLException when the container connection cannot be opened
   */
  public static Connection open(TestDatabaseOwnership.Registration ownership)
      throws SQLException {
    MySQLContainer<?> container = TestDatabaseOwnership.require(ownership);
    TestDatabaseUrlGuard.requireOwnedContainer(
        ownership,
        container.getJdbcUrl(),
        container.getUsername(),
        container.getPassword(),
        "test-container");
    return DriverManager.getConnection(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }
}
