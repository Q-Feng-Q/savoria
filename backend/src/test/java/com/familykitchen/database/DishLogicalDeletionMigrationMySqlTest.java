package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.testsupport.SafeTestFlyway;
import com.familykitchen.testsupport.SafeTestJdbc;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Applies V12 after a real V11 schema and verifies legacy data remains intact. */
@Testcontainers(disabledWithoutDocker = true)
class DishLogicalDeletionMigrationMySqlTest {

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_kitchen_dish_delete_test")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");
  private static final com.familykitchen.testsupport.TestDatabaseOwnership.Registration MYSQL_OWNER =
      com.familykitchen.testsupport.TestDatabaseOwnership.register(MYSQL);

  @BeforeAll
  static void migrateAroundLegacyRows() throws Exception {
    SafeTestFlyway.configure(MYSQL_OWNER).locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion("11")).load().migrate();
    try (Connection connection = SafeTestJdbc.open(MYSQL_OWNER);
         Statement statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO merchants (id,name,status) VALUES (91,'merchant-91','active')");
      statement.executeUpdate("INSERT INTO dish_categories (id,merchant_id,name) VALUES (910,91,'category-91')");
      statement.executeUpdate("INSERT INTO dishes (id,merchant_id,category_id,name,base_price,status) VALUES "
          + "(911,91,910,'active-legacy',11,'active'),(912,91,910,'inactive-legacy',12,'inactive')");
    }
    SafeTestFlyway.configure(MYSQL_OWNER).locations("classpath:db/migration").load().migrate();
  }

  @Test
  void addsNullableMicrosecondAuditColumnsWithoutChangingLegacyRows() throws Exception {
    try (Connection connection = SafeTestJdbc.open(MYSQL_OWNER);
         Statement statement = connection.createStatement()) {
      try (ResultSet columns = statement.executeQuery(
          "SELECT column_name,data_type,datetime_precision,is_nullable FROM information_schema.columns "
              + "WHERE table_schema=DATABASE() AND table_name='dishes' "
              + "AND column_name IN ('deleted_at','deleted_by') ORDER BY ordinal_position")) {
        assertTrue(columns.next());
        assertEquals("deleted_at", columns.getString("column_name"));
        assertEquals("datetime", columns.getString("data_type"));
        assertEquals(6, columns.getInt("datetime_precision"));
        assertEquals("YES", columns.getString("is_nullable"));
        assertTrue(columns.next());
        assertEquals("deleted_by", columns.getString("column_name"));
        assertEquals("bigint", columns.getString("data_type"));
        assertEquals("YES", columns.getString("is_nullable"));
      }
      try (ResultSet rows = statement.executeQuery(
          "SELECT status,deleted_at,deleted_by FROM dishes WHERE id IN (911,912) ORDER BY id")) {
        assertTrue(rows.next());
        assertEquals("active", rows.getString("status"));
        assertNull(rows.getObject("deleted_at"));
        assertNull(rows.getObject("deleted_by"));
        assertTrue(rows.next());
        assertEquals("inactive", rows.getString("status"));
        assertNull(rows.getObject("deleted_at"));
        assertNull(rows.getObject("deleted_by"));
      }
    }
  }

  @Test
  void createsExactOrderedScopeIndex() throws Exception {
    try (Connection connection = SafeTestJdbc.open(MYSQL_OWNER);
         Statement statement = connection.createStatement();
         ResultSet rows = statement.executeQuery(
             "SELECT column_name FROM information_schema.statistics WHERE table_schema=DATABASE() "
                 + "AND table_name='dishes' AND index_name='idx_dishes_merchant_status_deleted' "
                 + "ORDER BY seq_in_index")) {
      List<String> columns = new ArrayList<>();
      while (rows.next()) columns.add(rows.getString(1));
      assertEquals(List.of("merchant_id", "status", "deleted_at", "id"), columns);
    }
  }
}
