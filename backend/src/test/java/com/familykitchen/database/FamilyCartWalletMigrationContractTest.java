package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies that V11 is additive compatibility groundwork for shared carts and wallets. */
class FamilyCartWalletMigrationContractTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V11__add_family_cart_time_and_wallet.sql");

  @Test
  void makesRetiredLegacyColumnsNullableAndAddsCartAndOrderCompatibilityFields() throws Exception {
    String sql = normalizedSql();

    assertTrue(sql.contains("modify column user_id bigint null"));
    assertTrue(sql.contains("modify column meal_slot_id bigint null"));
    assertTrue(sql.contains("modify column service_date date null"));
    assertTrue(sql.contains("add column expected_meal_time datetime null"));
    assertTrue(sql.contains("add column version bigint not null default 0"));
    assertTrue(sql.contains("modify column delivery_fee_payer_user_id bigint null"));
    assertTrue(sql.contains("modify column owner_user_id bigint null"));
    assertTrue(sql.contains("add column source_cart_id bigint null"));
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
        "available_before decimal(18,2) not null", "available_after decimal(18,2) not null",
        "frozen_before decimal(18,2) not null", "frozen_after decimal(18,2) not null");
    assertTableHas(sql, "family_wallet_order_holds", "order_id bigint not null",
        "unique key uk_family_wallet_order_holds_order (order_id)",
        "initial_amount decimal(18,2) not null", "additional_frozen_amount decimal(18,2) not null",
        "remaining_frozen_amount decimal(18,2) not null", "captured_amount decimal(18,2) not null",
        "released_amount decimal(18,2) not null", "refunded_amount decimal(18,2) not null",
        "status varchar(30) not null", "check (refunded_amount <= captured_amount)",
        "check (initial_amount + additional_frozen_amount = remaining_frozen_amount + captured_amount + released_amount)");
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
        "cutover_epoch bigint not null", "drain_epoch bigint not null");
    assertTableHas(sql, "application_instance_leases", "instance_id varchar(128) not null",
        "build_version varchar(128) not null", "heartbeat_at datetime not null", "lease_expires_at datetime not null");
  }

  @Test
  void remainsAdditiveAndPreservesLegacyAndFinalizationStructures() throws Exception {
    String sql = normalizedSql();

    assertFalse(sql.contains("uk_carts_active_family"));
    assertFalse(sql.matches("(?s).*(^|;)\\s*(drop|truncate|delete|update)\\s+.*"));
    assertFalse(sql.contains("alter table meal_slots"));
    assertFalse(sql.contains("alter table member_wallets"));
    assertFalse(sql.contains("alter table wallet_ledgers"));
    assertFalse(sql.contains("alter table order_member_charges"));
    assertFalse(sql.contains("drop index uk_carts_active_cart"));
    assertFalse(sql.contains("drop column active_cart_key"));
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
        .toLowerCase();
  }
}
