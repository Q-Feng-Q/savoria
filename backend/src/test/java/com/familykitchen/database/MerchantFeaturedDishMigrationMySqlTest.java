package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.testsupport.SafeTestFlyway;
import java.math.BigDecimal;
import java.sql.Connection;
import com.familykitchen.testsupport.SafeTestJdbc;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Uses MySQL 8 to verify V10's deterministic legacy-data backfill. */
@Testcontainers(disabledWithoutDocker = true)
class MerchantFeaturedDishMigrationMySqlTest {

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_kitchen_featured_test")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");
  private static final com.familykitchen.testsupport.TestDatabaseOwnership.Registration MYSQL_OWNER =
      com.familykitchen.testsupport.TestDatabaseOwnership.register(MYSQL);

  @BeforeAll
  static void migrateAroundLegacyFixture() throws Exception {
    SafeTestFlyway.configure(MYSQL_OWNER)
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion("9"))
        .load()
        .migrate();
    seedLegacyData();
    SafeTestFlyway.configure(MYSQL_OWNER)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Test
  void backfillsTopFiveAndUsesDishIdToBreakEqualTimestampTies() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      List<Long> featured = new ArrayList<>();
      try (ResultSet rows = statement.executeQuery(
          "SELECT id FROM dishes WHERE merchant_id=21 AND featured_at IS NOT NULL "
              + "ORDER BY featured_at DESC,id DESC")) {
        while (rows.next()) featured.add(rows.getLong(1));
      }
      assertEquals(List.of(306L, 305L, 304L, 303L, 301L), featured);
      assertEquals(0L, longValue(statement,
          "SELECT COUNT(*) FROM dishes WHERE id IN (302,307,308,401) AND featured_at IS NOT NULL"));
      assertEquals(1L, longValue(statement,
          "SELECT COUNT(DISTINCT featured_at) FROM dishes WHERE merchant_id=21 AND featured_at IS NOT NULL"));
      assertTrue(indexExists(statement, "dishes", "idx_dishes_merchant_featured"));
    }
  }

  @Test
  void enablesSelectedDishesForAllActiveFamiliesAndPreservesExistingCustomization() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      try (ResultSet existing = statement.executeQuery(
          "SELECT enabled,sort_order,final_price FROM family_menu_items WHERE family_id=211 AND dish_id=301")) {
        assertTrue(existing.next());
        assertTrue(existing.getBoolean("enabled"));
        assertEquals(7, existing.getInt("sort_order"));
        assertEquals(new BigDecimal("99.00"), existing.getBigDecimal("final_price"));
      }
      try (ResultSet inserted = statement.executeQuery(
          "SELECT enabled,sort_order,final_price FROM family_menu_items WHERE family_id=211 AND dish_id=305")) {
        assertTrue(inserted.next());
        assertTrue(inserted.getBoolean("enabled"));
        assertEquals(31, inserted.getInt("sort_order"));
        assertEquals(new BigDecimal("15.00"), inserted.getBigDecimal("final_price"));
      }
      try (ResultSet appended = statement.executeQuery(
          "SELECT dish_id,sort_order FROM family_menu_items "
              + "WHERE family_id=211 AND dish_id IN (303,304,305) ORDER BY sort_order")) {
        assertTrue(appended.next());
        assertEquals(305L, appended.getLong("dish_id"));
        assertEquals(31, appended.getInt("sort_order"));
        assertTrue(appended.next());
        assertEquals(304L, appended.getLong("dish_id"));
        assertEquals(32, appended.getInt("sort_order"));
        assertTrue(appended.next());
        assertEquals(303L, appended.getLong("dish_id"));
        assertEquals(33, appended.getInt("sort_order"));
        assertFalse(appended.next());
      }
      assertEquals(5L, longValue(statement,
          "SELECT COUNT(*) FROM family_menu_items WHERE family_id=211 AND dish_id IN (301,303,304,305,306)"));
      assertEquals(0L, longValue(statement,
          "SELECT COUNT(*) FROM family_menu_items WHERE family_id=219 AND dish_id IN (301,303,304,305,306)"));
      assertFalse(booleanValue(statement,
          "SELECT enabled FROM family_menu_items WHERE family_id=211 AND dish_id=302"));
    }
  }

  private static void seedLegacyData() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO merchants (id,name,status) VALUES (21,'merchant-21','active'),(22,'merchant-22','active')");
      statement.executeUpdate("INSERT INTO dish_categories (id,merchant_id,name) VALUES (210,21,'category-21'),(220,22,'category-22')");
      statement.executeUpdate("INSERT INTO dishes (id,merchant_id,category_id,name,base_price,status) VALUES "
          + "(301,21,210,'dish-301',11,'active'),(302,21,210,'dish-302',12,'active'),"
          + "(303,21,210,'dish-303',13,'active'),(304,21,210,'dish-304',14,'active'),"
          + "(305,21,210,'dish-305',15,'active'),(306,21,210,'dish-306',16,'active'),"
          + "(307,21,210,'dish-307',17,'inactive'),(308,21,210,'dish-308',18,'active'),"
          + "(401,22,220,'dish-401',41,'active')");
      statement.executeUpdate("INSERT INTO families (id,merchant_id,name,status,featured_dish_id) VALUES "
          + "(211,21,'family-211','active',301),(212,21,'family-212','active',301),"
          + "(213,21,'family-213','active',302),(214,21,'family-214','active',303),"
          + "(215,21,'family-215','active',304),(216,21,'family-216','active',305),"
          + "(217,21,'family-217','active',306),(218,21,'family-218','active',307),"
          + "(219,21,'family-219','inactive',308),(220,21,'family-220','active',401)");
      statement.executeUpdate("INSERT INTO family_menu_items (family_id,dish_id,enabled,sort_order,final_price) VALUES "
          + "(211,301,0,7,99),(211,306,0,19,88),(211,302,0,30,77)");
    }
  }

  private static Connection open() throws SQLException {
    return SafeTestJdbc.open(MYSQL_OWNER);
  }

  private static Object singleValue(Statement statement, String sql) throws SQLException {
    try (ResultSet row = statement.executeQuery(sql)) {
      assertTrue(row.next());
      return row.getObject(1);
    }
  }

  private static long longValue(Statement statement, String sql) throws SQLException {
    Object value = singleValue(statement, sql);
    assertNotNull(value);
    return ((Number) value).longValue();
  }

  private static boolean booleanValue(Statement statement, String sql) throws SQLException {
    try (ResultSet row = statement.executeQuery(sql)) {
      assertTrue(row.next());
      return row.getBoolean(1);
    }
  }

  private static boolean indexExists(Statement statement, String tableName, String indexName)
      throws SQLException {
    try (ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM information_schema.statistics "
        + "WHERE table_schema=DATABASE() AND table_name='" + tableName + "' AND index_name='" + indexName + "'")) {
      row.next();
      return row.getInt(1) == 2;
    }
  }
}
