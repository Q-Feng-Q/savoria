package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 8 验证 JSON、CHECK 和生成列唯一约束。 */
@Testcontainers(disabledWithoutDocker = true)
class DishTemplateChangeMigrationMySqlTest {

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_kitchen_migration_test")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Test
  void migrationCreatesVersionColumnAndAcceptsJsonSnapshots() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      try (ResultSet result = statement.executeQuery(
          "SELECT version FROM dish_templates WHERE id=1")) {
        result.next();
        assertEquals(0L, result.getLong(1));
      }
      assertEquals(1, statement.executeUpdate(insertSql(11L, 1L, "PENDING")));
    }
  }

  @Test
  void checkConstraintRejectsUnknownStatus() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeUpdate(insertSql(12L, 1L, "UNKNOWN")));
    }
  }

  @Test
  void generatedColumnAllowsOnlyOnePendingRequestPerMerchantAndTemplate() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      assertEquals(1, statement.executeUpdate(insertSql(13L, 1L, "PENDING")));
      assertThrows(SQLException.class, () -> statement.executeUpdate(insertSql(13L, 1L, "PENDING")));
      assertEquals(1, statement.executeUpdate(insertSql(13L, 1L, "WITHDRAWN")));
    }
  }

  private static Connection open() throws SQLException {
    return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
  }

  private static String insertSql(long merchantId, long templateId, String status) {
    String snapshot = "{\"schemaVersion\":1}";
    return "INSERT INTO dish_template_change_requests "
        + "(merchant_id,template_id,base_template_version,base_snapshot_json,snapshot_json,status,submitted_by) "
        + "VALUES (" + merchantId + "," + templateId + ",0,'" + snapshot + "','" + snapshot + "','"
        + status + "',1)";
  }
}
