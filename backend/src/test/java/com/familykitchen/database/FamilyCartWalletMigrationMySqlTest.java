package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Uses an isolated MySQL 8 container to verify V11 compatibility and conservation. */
@Testcontainers(disabledWithoutDocker = true)
class FamilyCartWalletMigrationMySqlTest {

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_cart_wallet_v11_test")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");

  private static BigDecimal availableBefore;
  private static BigDecimal frozenBefore;
  private static String cartBefore;

  @BeforeAll
  static void migrateThroughV10ThenV11() throws Exception {
    Flyway.configure()
        .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion("10"))
        .load()
        .migrate();
    seedAndSnapshotLegacyRows();
    Flyway.configure()
        .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Test
  void leavesLegacyMoneyAndCartRowsByteForByteUnchanged() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      assertEquals(availableBefore, decimalValue(statement,
          "SELECT balance_amount FROM member_wallets WHERE user_id=701"));
      assertEquals(frozenBefore, decimalValue(statement,
          "SELECT frozen_amount FROM member_wallets WHERE user_id=701"));
      assertEquals(cartBefore, stringValue(statement,
          "SELECT CONCAT_WS('|',id,merchant_id,family_id,user_id,meal_slot_id,service_date,remark,status,active_cart_key) "
              + "FROM carts WHERE id=801"));
      assertEquals(1L, count(statement, "SELECT COUNT(*) FROM wallet_ledgers WHERE id=901"));
      assertEquals(1L, count(statement, "SELECT COUNT(*) FROM order_member_charges WHERE id=1001"));
    }
  }

  @Test
  void exposesNullableRetiredFieldsAndPreservesLegacyActiveCartIndex() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      assertNullable(statement, "carts", "user_id", true);
      assertNullable(statement, "carts", "meal_slot_id", true);
      assertNullable(statement, "carts", "service_date", true);
      assertNullable(statement, "orders", "meal_slot_id", true);
      assertNullable(statement, "orders", "service_date", true);
      assertNullable(statement, "orders", "delivery_fee_payer_user_id", true);
      assertNullable(statement, "order_items", "owner_user_id", true);
      assertTrue(indexExists(statement, "carts", "uk_carts_active_cart"));
      assertFalse(indexExists(statement, "carts", "uk_carts_active_family"));

      statement.executeUpdate("INSERT INTO carts "
          + "(id,merchant_id,family_id,user_id,meal_slot_id,service_date,expected_meal_time,status) "
          + "VALUES (802,61,501,NULL,NULL,NULL,'2026-09-01 18:45:00','active')");
      statement.executeUpdate("INSERT INTO orders "
          + "(id,merchant_id,family_id,submitter_user_id,meal_slot_id,service_date,delivery_mode,"
          + "delivery_fee_payer_user_id,status,source_cart_id,expected_meal_time) "
          + "VALUES (1101,61,501,701,NULL,NULL,'PICKUP',NULL,'CREATED',802,'2026-09-01 18:45:00')");
      statement.executeUpdate("INSERT INTO order_items "
          + "(id,order_id,dish_id,owner_user_id,price,quantity,amount) "
          + "VALUES (1201,1101,301,NULL,12.50,1,12.50)");
      assertEquals(0L, count(statement, "SELECT version FROM carts WHERE id=802"));
      assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO orders "
          + "(id,merchant_id,family_id,submitter_user_id,meal_slot_id,service_date,delivery_mode,"
          + "delivery_fee_payer_user_id,status,source_cart_id) "
          + "VALUES (1102,61,501,701,NULL,NULL,'PICKUP',NULL,'CREATED',802)"));
    }
  }

  @Test
  void enforcesSelectionUniquenessAndPositiveQuantityChecks() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO cart_item_selections (cart_item_id,user_id,quantity,item_remark) "
          + "VALUES (811,701,1,'less salt')");
      assertThrows(SQLException.class, () -> statement.executeUpdate(
          "INSERT INTO cart_item_selections (cart_item_id,user_id,quantity) VALUES (811,701,2)"));
      assertThrows(SQLException.class, () -> statement.executeUpdate(
          "INSERT INTO cart_item_selections (cart_item_id,user_id,quantity) VALUES (811,702,0)"));

      statement.executeUpdate("INSERT INTO order_item_selections "
          + "(order_item_id,user_id,quantity,member_name_snapshot,item_remark) "
          + "VALUES (1201,701,1,'member-701','less salt')");
      assertThrows(SQLException.class, () -> statement.executeUpdate(
          "INSERT INTO order_item_selections (order_item_id,user_id,quantity,member_name_snapshot) "
              + "VALUES (1201,701,1,'duplicate')"));
      assertThrows(SQLException.class, () -> statement.executeUpdate(
          "INSERT INTO order_item_selections (order_item_id,user_id,quantity,member_name_snapshot) "
              + "VALUES (1201,702,-1,'member-702')"));
    }
  }

  @Test
  void enforcesEveryFamilyWalletAndLedgerAmountGuard() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeUpdate(
          "INSERT INTO family_wallets (family_id,available_amount,frozen_amount) VALUES (502,-0.01,0)"));
      assertThrows(SQLException.class, () -> statement.executeUpdate(
          "INSERT INTO family_wallets (family_id,available_amount,frozen_amount) VALUES (503,0,-0.01)"));
      statement.executeUpdate(
          "INSERT INTO family_wallets (family_id,available_amount,frozen_amount) VALUES (501,100.00,20.00)");
      assertNullable(statement, "family_wallet_ledgers", "order_id", true);
      assertNullable(statement, "family_wallet_ledgers", "operator_user_id", true);
      assertTrue(indexExists(statement, "family_wallet_ledgers",
          "idx_family_wallet_ledgers_family_order"));
      assertTrue(indexExists(statement, "family_wallet_ledgers",
          "idx_family_wallet_ledgers_operator_created"));

      statement.executeUpdate("INSERT INTO family_wallet_ledgers "
          + "(family_id,scope_key,business_type,business_key,amount,available_before,available_after,"
          + "frozen_before,frozen_after) VALUES (501,'family:501','ORDER_HOLD','order:1101',20,100,80,0,20)");
      assertEquals(1L, count(statement, "SELECT COUNT(*) FROM family_wallet_ledgers "
          + "WHERE business_key='order:1101' AND order_id IS NULL AND operator_user_id IS NULL"));
      statement.executeUpdate("INSERT INTO family_wallet_ledgers "
          + "(family_id,order_id,operator_user_id,scope_key,business_type,business_key,amount,"
          + "available_before,available_after,frozen_before,frozen_after) "
          + "VALUES (501,1101,701,'family:501','AUDIT','audit:1101',0,80,80,20,20)");
      assertEquals(1101L, count(statement,
          "SELECT order_id FROM family_wallet_ledgers WHERE business_key='audit:1101'"));
      assertEquals(701L, count(statement,
          "SELECT operator_user_id FROM family_wallet_ledgers WHERE business_key='audit:1101'"));
      assertLedgerRejected(statement, "negative-amount", "-0.01", "100", "100", "0", "0");
      assertLedgerRejected(statement, "negative-available-before", "0", "-0.01", "0", "0", "0");
      assertLedgerRejected(statement, "negative-available-after", "0", "0", "-0.01", "0", "0");
      assertLedgerRejected(statement, "negative-frozen-before", "0", "0", "0", "-0.01", "0");
      assertLedgerRejected(statement, "negative-frozen-after", "0", "0", "0", "0", "-0.01");
      assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO family_wallet_ledgers "
          + "(family_id,scope_key,business_type,business_key,amount,available_before,available_after,"
          + "frozen_before,frozen_after) VALUES (501,NULL,'ORDER_HOLD','null-scope',0,0,0,0,0)"));
      assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO family_wallet_ledgers "
          + "(family_id,scope_key,business_type,business_key,amount,available_before,available_after,"
          + "frozen_before,frozen_after) VALUES (501,'family:501','ORDER_HOLD','order:1101',1,80,79,20,21)"));
    }
  }

  @Test
  void enforcesEveryOrderHoldAmountEquationRefundAndUniquenessGuard() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      for (String constraint : new String[] {"ck_family_wallet_order_holds_initial",
          "ck_family_wallet_order_holds_additional", "ck_family_wallet_order_holds_remaining",
          "ck_family_wallet_order_holds_captured", "ck_family_wallet_order_holds_released",
          "ck_family_wallet_order_holds_refunded", "ck_family_wallet_order_holds_equation",
          "ck_family_wallet_order_holds_refund"}) {
        assertTrue(checkConstraintExists(statement, "family_wallet_order_holds", constraint),
            () -> "Missing enforced check " + constraint);
      }
      statement.executeUpdate("INSERT INTO family_wallet_order_holds "
          + "(order_id,family_id,initial_amount,additional_frozen_amount,remaining_frozen_amount,"
          + "captured_amount,released_amount,refunded_amount,status) "
          + "VALUES (1101,501,20,5,10,10,5,2,'PARTIALLY_CAPTURED')");
      assertHoldRejected(statement, 1101, "20", "5", "10", "10", "5", "2");
      assertHoldRejected(statement, 1102, "-1", "2", "1", "0", "0", "0");
      assertHoldRejected(statement, 1103, "2", "-1", "1", "0", "0", "0");
      assertHoldRejected(statement, 1104, "1", "0", "-1", "2", "0", "0");
      assertHoldRejected(statement, 1105, "1", "0", "2", "-1", "0", "-2");
      assertHoldRejected(statement, 1106, "1", "0", "2", "0", "-1", "0");
      assertHoldRejected(statement, 1107, "1", "0", "1", "0", "0", "-1");
      assertHoldRejected(statement, 1108, "20", "0", "5", "10", "4", "0");
      assertHoldRejected(statement, 1109, "20", "0", "0", "20", "0", "21");
    }
  }

  @Test
  void enforcesEveryCommandIdempotencyScopeFieldAndCompositeUniqueness() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      assertNullable(statement, "command_idempotency", "actor_user_id", false);
      assertNullable(statement, "command_idempotency", "family_id", false);
      assertNullable(statement, "command_idempotency", "operation", false);
      assertNullable(statement, "command_idempotency", "request_id", false);
      assertNullable(statement, "command_idempotency", "payload_hash", false);
      statement.executeUpdate("INSERT INTO command_idempotency "
          + "(actor_user_id,family_id,operation,request_id,payload_hash,state) "
          + "VALUES (701,501,'SUBMIT_ORDER','request-1','hash-1','SUCCEEDED')");
      assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO command_idempotency "
          + "(actor_user_id,family_id,operation,request_id,payload_hash,state) "
          + "VALUES (701,501,'SUBMIT_ORDER','request-1','hash-2','STARTED')"));
      assertCommandScopeRejected(statement, "NULL", "501", "'SUBMIT_ORDER'", "'request-null-actor'");
      assertCommandScopeRejected(statement, "701", "NULL", "'SUBMIT_ORDER'", "'request-null-family'");
      assertCommandScopeRejected(statement, "701", "501", "NULL", "'request-null-operation'");
      assertCommandScopeRejected(statement, "701", "501", "'SUBMIT_ORDER'", "NULL");
      assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO command_idempotency "
          + "(actor_user_id,family_id,operation,request_id,payload_hash,state) "
          + "VALUES (701,501,'SUBMIT_ORDER','request-null-payload',NULL,'STARTED')"));
    }
  }

  @Test
  void createsDurableMigrationAndInstanceCoordinationTables() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      for (String table : new String[] {"family_wallet_migration_batches",
          "family_wallet_migration_sources", "family_cart_migration_sources",
          "family_wallet_migration_anomalies", "family_wallet_cutover_state",
          "application_instance_leases"}) {
        assertTrue(tableExists(statement, table), () -> "Missing table " + table);
      }
    }
  }

  private static void assertLedgerRejected(Statement statement, String businessKey, String amount,
      String availableBefore, String availableAfter, String frozenBefore, String frozenAfter) {
    assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO family_wallet_ledgers "
        + "(family_id,scope_key,business_type,business_key,amount,available_before,available_after,"
        + "frozen_before,frozen_after) VALUES (501,'family:501','GUARD_TEST','" + businessKey + "',"
        + amount + "," + availableBefore + "," + availableAfter + "," + frozenBefore + "," + frozenAfter + ")"));
  }

  private static void assertHoldRejected(Statement statement, long orderId, String initial,
      String additional, String remaining, String captured, String released, String refunded) {
    assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO family_wallet_order_holds "
        + "(order_id,family_id,initial_amount,additional_frozen_amount,remaining_frozen_amount,"
        + "captured_amount,released_amount,refunded_amount,status) VALUES (" + orderId + ",501,"
        + initial + "," + additional + "," + remaining + "," + captured + "," + released + ","
        + refunded + ",'GUARD_TEST')"));
  }

  private static void assertCommandScopeRejected(Statement statement, String actorUserId,
      String familyId, String operation, String requestId) {
    assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO command_idempotency "
        + "(actor_user_id,family_id,operation,request_id,payload_hash,state) VALUES ("
        + actorUserId + "," + familyId + "," + operation + "," + requestId + ",'hash','STARTED')"));
  }

  private static boolean checkConstraintExists(Statement statement, String table, String constraint)
      throws SQLException {
    return count(statement, "SELECT COUNT(*) FROM information_schema.table_constraints "
        + "WHERE constraint_schema=DATABASE() AND table_name='" + table + "' "
        + "AND constraint_name='" + constraint + "' AND constraint_type='CHECK' "
        + "AND enforced='YES'") == 1;
  }

  private static void seedAndSnapshotLegacyRows() throws Exception {
    try (Connection connection = open(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO member_wallets (user_id,balance_amount,frozen_amount) "
          + "VALUES (701,123.45,6.78)");
      statement.executeUpdate("INSERT INTO carts "
          + "(id,merchant_id,family_id,user_id,meal_slot_id,service_date,remark,status) "
          + "VALUES (801,61,501,701,601,'2026-08-30','legacy-cart','active')");
      statement.executeUpdate("INSERT INTO cart_items "
          + "(id,cart_id,dish_id,price,quantity) VALUES (811,801,301,12.50,2)");
      statement.executeUpdate("INSERT INTO orders "
          + "(id,merchant_id,family_id,submitter_user_id,meal_slot_id,service_date,delivery_mode,"
          + "delivery_fee_payer_user_id,status,total_amount) "
          + "VALUES (1001,61,501,701,601,'2026-08-30','PICKUP',701,'CREATED',25.00)");
      statement.executeUpdate("INSERT INTO order_member_charges "
          + "(id,order_id,user_id,dish_amount,total_amount,status) "
          + "VALUES (1001,1001,701,25.00,25.00,'FROZEN')");
      statement.executeUpdate("INSERT INTO wallet_ledgers "
          + "(id,user_id,order_id,type,amount,balance_before,balance_after,frozen_before,frozen_after) "
          + "VALUES (901,701,1001,'FREEZE',25.00,148.45,123.45,0.00,6.78)");
      availableBefore = decimalValue(statement,
          "SELECT balance_amount FROM member_wallets WHERE user_id=701");
      frozenBefore = decimalValue(statement,
          "SELECT frozen_amount FROM member_wallets WHERE user_id=701");
      cartBefore = stringValue(statement,
          "SELECT CONCAT_WS('|',id,merchant_id,family_id,user_id,meal_slot_id,service_date,remark,status,active_cart_key) "
              + "FROM carts WHERE id=801");
    }
  }

  private static Connection open() throws SQLException {
    return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
  }

  private static void assertNullable(Statement statement, String table, String column, boolean expected)
      throws SQLException {
    assertEquals(expected ? "YES" : "NO", stringValue(statement,
        "SELECT is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() "
            + "AND table_name='" + table + "' AND column_name='" + column + "'"));
  }

  private static boolean tableExists(Statement statement, String table) throws SQLException {
    return count(statement, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() "
        + "AND table_name='" + table + "'") == 1;
  }

  private static boolean indexExists(Statement statement, String table, String index) throws SQLException {
    return count(statement, "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() "
        + "AND table_name='" + table + "' AND index_name='" + index + "'") > 0;
  }

  private static long count(Statement statement, String sql) throws SQLException {
    try (ResultSet row = statement.executeQuery(sql)) {
      assertTrue(row.next());
      return row.getLong(1);
    }
  }

  private static BigDecimal decimalValue(Statement statement, String sql) throws SQLException {
    try (ResultSet row = statement.executeQuery(sql)) {
      assertTrue(row.next());
      return row.getBigDecimal(1);
    }
  }

  private static String stringValue(Statement statement, String sql) throws SQLException {
    try (ResultSet row = statement.executeQuery(sql)) {
      assertTrue(row.next());
      return row.getString(1);
    }
  }
}
