package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.migration.FamilyCartWalletMigrationRunner;
import com.familykitchen.migration.FamilyCartWalletMigrationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies resumability and conservation against a disposable MySQL container. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = FamilyCartWalletMigrationRecoveryMySqlTest.TestApp.class,
    properties = {"spring.flyway.enabled=false", "family-kitchen.migration.mode=OFF"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FamilyCartWalletMigrationRecoveryMySqlTest {

  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("runner_family_wallet_disposable")
      .withUsername("runner_test")
      .withPassword("runner_test");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired FamilyCartWalletMigrationService service;

  @BeforeEach
  void resetDatabase() {
    Flyway flyway = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
        .locations("classpath:db/migration").cleanDisabled(false).load();
    flyway.clean();
    flyway.migrate();
    seedTwoFamilies();
  }

  @Test
  void defaultOffPerformsNoWritesAndPreflightReportsAnomalies() {
    long before = count("family_wallet_migration_batches");
    runner("OFF", null, null).run(new DefaultApplicationArguments());
    assertEquals(before, count("family_wallet_migration_batches"));

    jdbc.update("update family_user_relations set status='EXITED' where user_id=12");
    FamilyCartWalletMigrationService.Result result = service.preflight(null, null);
    assertTrue(result.anomalyCount() > 0);
    assertEquals(0, count("family_wallet_migration_sources"));
  }

  @Test
  void quiesceRejectsWrongBatchAndEpochAndAbortIsPermanentAfterFirstSource() {
    long batch = service.preflight(null, null).batchId();
    assertThrows(IllegalStateException.class, () -> service.quiesce(batch + 1, "runner"));
    long epoch = service.quiesce(batch, "runner").drainEpoch();
    assertThrows(IllegalStateException.class,
        () -> service.validateContinuation(batch, epoch + 1, FamilyCartWalletMigrationRunner.Mode.EXECUTE));
    service.abortBeforeExecute(batch, epoch);
    assertFalse(Boolean.TRUE.equals(jdbc.queryForObject(
        "select maintenance_enabled from family_wallet_cutover_state where scope_key='GLOBAL'", Boolean.class)));
  }

  @Test
  void failedFamilyRollsBackAndSameBatchResumeDoesNotDoubleCredit() {
    long batch = service.preflight(null, null).batchId();
    long epoch = service.quiesce(batch, "runner").drainEpoch();
    service.preflight(batch, epoch);
    assertTrue(service.executeFamily(batch, epoch, 101L));
    BigDecimal credited = money("select available_amount from family_wallets where family_id=101");
    assertTrue(service.executeFamily(batch, epoch, 101L));
    assertEquals(credited, money("select available_amount from family_wallets where family_id=101"));
    assertEquals(2, count("family_wallet_migration_sources where family_id=101"));
    assertThrows(IllegalStateException.class, () -> service.abortBeforeExecute(batch, epoch));
  }

  @Test
  void cartMergeUsesCurrentPriceAndMemberSelectionsThenVerifyAndFinalizeAreResumable() {
    long batch = service.preflight(null, null).batchId();
    long epoch = service.quiesce(batch, "runner").drainEpoch();
    service.preflight(batch, epoch);
    assertTrue(service.executeFamily(batch, epoch, 101L));
    assertEquals(new BigDecimal("15.50"), money("select price from cart_items where cart_id=1001 and dish_id=501"));
    assertEquals(3L, jdbc.queryForObject("select sum(quantity) from cart_item_selections "
        + "where cart_item_id=(select id from cart_items where cart_id=1001 and dish_id=501)", Long.class));
    assertEquals("migrated", jdbc.queryForObject("select status from carts where id=1002", String.class));

    assertTrue(service.executeFamily(batch, epoch, 102L));
    service.markExecutionComplete(batch, epoch);
    assertEquals("VERIFIED", service.verify(batch, epoch).status());
    service.finalizeBatch(batch, epoch);
    service.finalizeBatch(batch, epoch);
    assertEquals("FAMILY_READY", jdbc.queryForObject(
        "select state from family_wallet_cutover_state where scope_key='GLOBAL'", String.class));
    assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.statistics "
        + "where table_schema=database() and table_name='carts' and index_name='uk_carts_active_family'", Integer.class));
  }

  private FamilyCartWalletMigrationRunner runner(String mode, Long batch, Long epoch) {
    return new FamilyCartWalletMigrationRunner(service, mode, batch, epoch,
        "runner_family_wallet_disposable", "single-use-test-token", "single-use-test-token");
  }

  private long count(String tableAndWhere) {
    return jdbc.queryForObject("select count(*) from " + tableAndWhere, Long.class);
  }

  private BigDecimal money(String sql) {
    return jdbc.queryForObject(sql, BigDecimal.class);
  }

  private void seedTwoFamilies() {
    jdbc.update("insert into users(id,username,password_hash,nickname) values(11,'u11','x','u11'),(12,'u12','x','u12'),(13,'u13','x','u13')");
    jdbc.update("insert into merchants(id,name,status) values(91,'m','active')");
    jdbc.update("insert into families(id,merchant_id,name,status) values(101,91,'f1','active'),(102,91,'f2','active')");
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) values(11,101,'OWNER','ACTIVE','TEST'),(12,101,'MEMBER','ACTIVE','TEST'),(13,102,'OWNER','ACTIVE','TEST')");
    jdbc.update("insert into member_wallets(user_id,balance_amount,frozen_amount) values(11,10,0),(12,20,0),(13,7,0)");
    jdbc.update("insert into dish_categories(id,merchant_id,name) values(401,91,'c')");
    jdbc.update("insert into dishes(id,merchant_id,category_id,name,base_price,status) values(501,91,401,'dish',15.50,'active')");
    jdbc.update("insert into family_menu_items(family_id,dish_id,enabled,final_price) values(101,501,1,15.50)");
    jdbc.update("insert into meal_slots(id,family_id,name,enabled) values(601,101,'dinner',1)");
    jdbc.update("insert into carts(id,merchant_id,family_id,user_id,meal_slot_id,service_date,remark,status,updated_at) values(1001,91,101,11,601,?,'old','active','2026-01-01'),(1002,91,101,12,601,?,'new','active','2026-01-02')", LocalDate.now(), LocalDate.now());
    jdbc.update("insert into cart_items(id,cart_id,dish_id,price,quantity,item_remark,updated_at) values(1101,1001,501,9,1,'a','2026-01-01'),(1102,1002,501,9,2,'b','2026-01-02')");
  }

  /** Minimal application used by the migration recovery test. */
  @SpringBootApplication
  @MapperScan("com.familykitchen.migration")
  @Import(TestConfig.class)
  static class TestApp { }

  /** Supplies migration services without loading unrelated application services. */
  @TestConfiguration
  static class TestConfig {
    @Bean com.familykitchen.migration.FamilyCartWalletFamilyExecutor familyExecutor(
        com.familykitchen.migration.FamilyCartWalletMigrationMapper mapper) {
      return new com.familykitchen.migration.FamilyCartWalletFamilyExecutor(mapper);
    }

    @Bean FamilyCartWalletMigrationService migrationService(
        com.familykitchen.migration.FamilyCartWalletMigrationMapper mapper,
        com.familykitchen.migration.FamilyCartWalletFamilyExecutor executor) {
      return new FamilyCartWalletMigrationService(mapper, executor);
    }
  }
}
