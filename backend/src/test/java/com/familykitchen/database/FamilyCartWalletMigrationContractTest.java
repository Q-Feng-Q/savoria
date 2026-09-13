package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证家庭共享餐篮和钱包已经进入最终初始化结构。 */
class FamilyCartWalletMigrationContractTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V1__init_schema.sql");

  @Test
  void declaresFinalCartAndOrderCompatibilityFields() throws Exception {
    String sql = normalizedSql();

    assertTrue(sql.contains("user_id bigint null"));
    assertTrue(sql.contains("meal_slot_id bigint null"));
    assertTrue(sql.contains("service_date date null"));
    assertTrue(sql.contains("expected_meal_time datetime null"));
    assertTrue(sql.contains("version bigint not null default '0'"));
    assertTrue(sql.contains("delivery_fee_payer_user_id bigint null"));
    assertTrue(sql.contains("owner_user_id bigint null"));
    assertTrue(sql.contains("source_cart_id bigint null"));
    assertTrue(sql.contains("unique key uk_orders_source_cart (source_cart_id)"));
  }

  @Test
  void createsPerMemberSelectionTablesWithPositiveQuantities() throws Exception {
    String sql = normalizedSql();

    assertTableHas(sql, "cart_item_selections", "cart_item_id bigint not null",
        "user_id bigint not null", "quantity int not null", "item_remark varchar(255) null",
        "unique key uk_cart_item_selections_item_user (cart_item_id,user_id)",
        "check (quantity > 0)", "created_at datetime not null", "updated_at datetime not null");
    assertTableHas(sql, "order_item_selections", "order_item_id bigint not null",
        "user_id bigint not null", "quantity int not null", "member_name_snapshot varchar(100) not null",
        "item_remark varchar(255) null",
        "unique key uk_order_item_selections_item_user (order_item_id,user_id)",
        "check (quantity > 0)", "created_at datetime not null", "updated_at datetime not null");
  }

  @Test
  void createsCheckedFamilyWalletLedgerAndOrderHoldTables() throws Exception {
    String sql = normalizedSql();

    assertTableHas(sql, "family_wallets", "family_id bigint not null",
        "available_amount decimal(18,2) not null", "frozen_amount decimal(18,2) not null",
        "check (available_amount >= 0)", "check (frozen_amount >= 0)");
    assertTableHas(sql, "family_wallet_ledgers", "family_id bigint not null",
        "scope_key varchar(100) not null", "business_type varchar(40) not null",
        "business_key varchar(128) not null",
        "unique key uk_family_wallet_ledgers_business (business_type,business_key)",
        "amount decimal(18,2) not null", "check (amount >= 0)",
        "available_before decimal(18,2) not null", "check (available_before >= 0)",
        "available_after decimal(18,2) not null", "check (available_after >= 0)",
        "frozen_before decimal(18,2) not null", "check (frozen_before >= 0)",
        "order_id bigint null", "operator_user_id bigint null",
        "key idx_family_wallet_ledgers_family_order (family_id,order_id)",
        "key idx_family_wallet_ledgers_operator_created (operator_user_id,created_at)",
        "frozen_after decimal(18,2) not null", "check (frozen_after >= 0)");
    assertTableHas(sql, "family_wallet_order_holds", "order_id bigint not null",
        "unique key uk_family_wallet_order_holds_order (order_id)",
        "initial_amount decimal(18,2) not null", "additional_frozen_amount decimal(18,2) not null",
        "remaining_frozen_amount decimal(18,2) not null", "captured_amount decimal(18,2) not null",
        "released_amount decimal(18,2) not null", "refunded_amount decimal(18,2) not null",
        "status varchar(30) not null", "check (refunded_amount <= captured_amount)",
        "check (initial_amount >= 0)",
        "check (additional_frozen_amount >= 0)",
        "check (remaining_frozen_amount >= 0)",
        "check (captured_amount >= 0)",
        "check (released_amount >= 0)",
        "check (refunded_amount >= 0)",
        "initial_amount + additional_frozen_amount",
        "remaining_frozen_amount + captured_amount");
  }

  @Test
  void createsDurableNormalizedCommandIdempotency() throws Exception {
    String sql = normalizedSql();

    assertTableHas(sql, "command_idempotency", "actor_user_id bigint not null",
        "family_id bigint not null", "operation varchar(80) not null", "request_id varchar(128) not null",
        "payload_hash varchar(128) not null", "state varchar(30) not null",
        "result_resource_type varchar(80) null", "result_resource_id bigint null", "result_body json null",
        "unique key uk_command_idempotency_scope (actor_user_id,family_id,operation,request_id)");
  }

  @Test
  void createsDurableMigrationAndDeploymentCoordinationMetadata() throws Exception {
    String sql = normalizedSql();

    assertTableHas(sql, "family_wallet_migration_batches", "phase varchar(30) not null",
        "cutover_epoch bigint not null", "drain_epoch bigint not null",
        "source_available_total decimal(18,2) not null", "source_frozen_total decimal(18,2) not null",
        "target_available_total decimal(18,2) not null", "target_frozen_total decimal(18,2) not null",
        "status varchar(30) not null");
    assertTableHas(sql, "family_wallet_migration_sources", "source_user_id bigint not null",
        "unique key uk_family_wallet_migration_source (batch_id,source_user_id)");
    assertTableHas(sql, "family_cart_migration_sources", "source_cart_id bigint not null",
        "unique key uk_family_cart_migration_source (batch_id,source_cart_id)");
    assertTableHas(sql, "family_wallet_migration_anomalies", "batch_id bigint not null",
        "anomaly_type varchar(80) not null", "detail_json json not null");
    assertTableHas(sql, "family_wallet_cutover_state", "maintenance_enabled tinyint(1) not null",
        "cutover_epoch bigint not null", "drain_epoch bigint not null", "active_batch_id bigint null");
    assertTableHas(sql, "application_instance_leases", "instance_id varchar(128) not null",
        "build_version varchar(128) not null", "heartbeat_at datetime not null", "lease_expires_at datetime not null");
    assertTableHas(sql, "family_wallet_migration_runner_lease", "scope_key varchar(100) not null",
        "owner_token varchar(128) not null", "batch_id bigint not null", "drain_epoch bigint not null",
        "mode varchar(30) not null", "lease_expires_at datetime not null");
    assertTableHas(sql, "family_wallet_migration_families", "batch_id bigint not null",
        "family_id bigint not null", "status varchar(30) not null",
        "unique key uk_family_wallet_migration_family (batch_id,family_id)");
  }

  @Test
  void finalSchemaContainsNoIncrementalOrDataChangingStatements() throws Exception {
    String sql = normalizedSql();

    assertNoDataChangingSql(sql);
    assertFalse(sql.contains("alter table"));
    assertFalse(sql.contains("drop table"));
  }

  @Test
  void additiveGuardRejectsDataChangesAndSelectBasedCopiesButAllowsTimestampClauses() {
    String[] forbidden = {
        "INSERT INTO member_wallets SELECT * FROM old_wallets;",
        "REPLACE INTO carts VALUES (1);",
        "UPDATE member_wallets SET balance_amount=0;",
        "DELETE FROM carts;",
        "TRUNCATE TABLE order_member_charges;",
        "DROP TABLE member_wallets;",
        "SELECT * INTO copied_wallets FROM member_wallets;",
        "MERGE INTO carts USING legacy_carts ON carts.id=legacy_carts.id WHEN MATCHED THEN DELETE;",
        "ALTER TABLE carts DROP COLUMN user_id;",
        "ALTER TABLE carts RENAME COLUMN user_id TO legacy_user_id;",
        "ALTER TABLE carts MODIFY COLUMN status varchar(40) NULL;",
        "CALL migrate_money();",
        "EXECUTE migrate_statement;",
        "PREPARE migrate_statement FROM 'UPDATE carts SET status=1';",
        "LOAD DATA INFILE 'wallets.csv' INTO TABLE member_wallets;",
        "CREATE PROCEDURE migrate_money() UPDATE member_wallets SET balance_amount=0;",
        "CREATE TABLE copied_wallets AS SELECT * FROM member_wallets;"
    };
    for (String sql : forbidden) {
      assertThrows(AssertionError.class, () -> assertNoDataChangingSql(sql), sql);
    }
    assertNoDataChangingSql("CREATE TABLE safe_table (updated_at datetime NOT NULL "
        + "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);");
  }

  private static void assertNoDataChangingSql(String sql) {
    String normalized = sql
        .replaceAll("--[^\\r\\n]*", " ")
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase();
    assertFalse(normalized.matches(
        "(?s).*(^|;)\\s*(insert(?:\\s+ignore)?\\s+into|replace(?:\\s+into)?|update|"
            + "delete(?:\\s+from)?|truncate(?:\\s+table)?|drop|merge(?:\\s+into)?|call|execute|"
            + "prepare|load\\s+data)\\b.*"),
        "Migration must not execute data-changing statements");
    assertFalse(normalized.matches("(?s).*\\bselect\\b.*"),
        "Schema-only migration must not read or copy legacy rows");
    assertFalse(normalized.matches(
        "(?s).*\\balter\\s+table\\b[^;]*\\b(drop|rename|change)\\b.*"),
        "Compatibility migration must not drop, rename, or change legacy columns");
    assertFalse(normalized.matches(
        "(?s).*(^|;)\\s*create\\s+(procedure|function|trigger|event)\\b.*"),
        "Migration must not hide data changes in stored database code");
    assertFalse(normalized.matches(
        "(?s).*\\bcreate\\s+table\\b.*\\b(?:as\\s+)?select\\b.*"),
        "Migration must not copy data with CREATE TABLE AS SELECT");
    for (String table : new String[] {"member_wallets", "carts", "order_member_charges"}) {
      assertFalse(normalized.matches(
          "(?s).*\\b(?:insert(?:\\s+ignore)?\\s+into|replace(?:\\s+into)?|update|delete\\s+from|"
              + "truncate\\s+table|merge\\s+into)\\s+`?" + table + "`?\\b.*"),
          () -> "Migration must not write legacy table " + table);
    }
    java.util.Set<String> allowedModifications = java.util.Set.of(
        "carts.user_id",
        "carts.meal_slot_id",
        "carts.service_date",
        "orders.meal_slot_id",
        "orders.service_date",
        "orders.delivery_fee_payer_user_id",
        "order_items.owner_user_id");
    java.util.regex.Matcher alter = java.util.regex.Pattern.compile(
        "(?s)\\balter\\s+table\\s+`?([a-z0-9_]+)`?\\s+([^;]*);").matcher(normalized);
    while (alter.find()) {
      String table = alter.group(1);
      java.util.regex.Matcher modify = java.util.regex.Pattern.compile(
          "\\bmodify\\s+column\\s+`?([a-z0-9_]+)`?").matcher(alter.group(2));
      while (modify.find()) {
        String target = table + "." + modify.group(1);
        assertTrue(allowedModifications.contains(target),
            () -> "Migration may only relax an approved retired field: " + target);
      }
    }
  }

  private static void assertTableHas(String sql, String table, String... fragments) {
    int start = sql.indexOf("create table " + table + " (");
    assertTrue(start >= 0, () -> "Missing table " + table);
    int end = sql.indexOf(") engine=innodb", start);
    assertTrue(end > start, () -> "Cannot find end of table " + table);
    String definition = sql.substring(start, end);
    for (String fragment : fragments) {
      assertTrue(definition.contains(fragment), () -> table + " must contain: " + fragment);
    }
  }

  private static String normalizedSql() throws Exception {
    assertTrue(Files.exists(MIGRATION), () -> "Missing migration: " + MIGRATION);
    return Files.readString(MIGRATION, StandardCharsets.UTF_8)
        .replaceAll("--[^\\r\\n]*", " ")
        .replaceAll("\\s*,\\s*", ",")
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase()
        .replaceAll("constraint [a-z0-9_]+ check", "check")
        .replace("check ((", "check (")
        .replace(" default null", " null");
  }
}
