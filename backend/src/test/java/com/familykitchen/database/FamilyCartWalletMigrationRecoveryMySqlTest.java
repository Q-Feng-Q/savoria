package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.migration.FamilyCartWalletDdlExecutor;
import com.familykitchen.migration.FamilyCartWalletFamilyExecutor;
import com.familykitchen.migration.FamilyCartWalletMigrationMapper;
import com.familykitchen.migration.FamilyCartWalletMigrationRunner;
import com.familykitchen.migration.FamilyCartWalletMigrationService;
import com.familykitchen.migration.FamilyWalletMigrationBarrierService;
import com.familykitchen.migration.FamilyWalletMigrationLeaseService;
import com.familykitchen.testsupport.SafeTestDatabaseProperties;
import com.familykitchen.testsupport.SafeTestFlyway;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Real MySQL recovery and concurrency tests; skipped honestly when Docker is unavailable. */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test-container")
@SpringBootTest(classes = FamilyCartWalletMigrationRecoveryMySqlTest.TestApp.class,
    properties = {"spring.flyway.enabled=false", "family-kitchen.migration.mode=OFF",
        "family-kitchen.instance.lease-enabled=false"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FamilyCartWalletMigrationRecoveryMySqlTest {
  @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("runner_family_wallet_disposable").withUsername("runner_test").withPassword("runner_test");

  @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
    SafeTestDatabaseProperties.register(registry, MYSQL);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired FamilyCartWalletMigrationService service;
  @Autowired FamilyWalletMigrationLeaseService leases;

  @BeforeEach void resetDatabase() {
    Flyway flyway = SafeTestFlyway.configure(MYSQL)
        .locations("classpath:db/migration").cleanDisabled(false).load();
    flyway.clean(); flyway.migrate(); seedTwoFamilies();
  }

  @Test void defaultOffPerformsNoMigrationLeaseBarrierOrBatchWrites() {
    runner("OFF", null, null).run(new DefaultApplicationArguments());
    assertEquals(0, count("family_wallet_migration_batches"));
    assertEquals(0, count("family_wallet_migration_runner_lease"));
    assertEquals(0, count("family_wallet_cutover_state"));
  }

  @Test void crossProcessLeaseRaceAllowsExactlyOneOwner() {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    List<String> owners = new ArrayList<>();
    try {
      List<Future<String>> attempts = List.of(
          pool.submit(() -> acquireAfter(start)), pool.submit(() -> acquireAfter(start)));
      start.countDown();
      for (Future<String> attempt : attempts) {
        try { owners.add(attempt.get()); } catch (Exception expected) { /* losing owner is rejected */ }
      }
      assertEquals(1, owners.size());
    } finally {
      owners.forEach(leases::release);
      pool.shutdownNow();
    }
  }

  @Test void barrierCommitsBeforeDrainAndAVisibleLeasePreventsProof() {
    long batch = preflight();
    jdbc.update("insert into application_instance_leases(instance_id,build_version,lease_owner,heartbeat_at,lease_expires_at) "
        + "values('web-1','compat','web',now(),date_add(now(),interval 30 second))");
    String token = leases.acquire(batch, 0, "QUIESCE");
    try {
      assertEquals("QUIESCING", service.quiesce(token, batch, "runner").status());
      assertEquals(1, jdbc.queryForObject("select maintenance_enabled from family_wallet_cutover_state where scope_key='GLOBAL'", Integer.class));
      jdbc.update("delete from application_instance_leases where instance_id='web-1'");
      assertEquals("DRAINED", service.quiesce(token, batch, "runner").status());
    } finally { leases.release(token); }
  }

  @Test void injectedMidFamilyFailureRollsBackAndResumeDoesNotDoubleCredit() {
    long batch = preflight(); long epoch = quiesce(batch); barrierPreflight(batch, epoch);
    jdbc.update("update dishes set status='inactive' where id=501");
    String token = leases.acquire(batch, epoch, "EXECUTE");
    try { assertThrows(IllegalStateException.class, () -> service.executeFamily(token, batch, epoch, 101L)); }
    finally { leases.release(token); }
    assertEquals(new BigDecimal("10.00"), money("select balance_amount from member_wallets where user_id=11"));
    assertEquals(0, count("family_wallet_migration_sources where family_id=101"));
    jdbc.update("update dishes set status='active' where id=501");
    execute(batch, epoch, 101); BigDecimal once = money("select available_amount from family_wallets where family_id=101");
    execute(batch, epoch, 101);
    assertEquals(once, money("select available_amount from family_wallets where family_id=101"));
  }

  @Test void staleDrainProofAndPhaseOrderAreRejected() {
    long batch = preflight();
    String verifyToken = leases.acquire(batch, 1, "VERIFY");
    try { assertThrows(IllegalStateException.class, () -> service.verify(verifyToken, batch, 1L)); }
    finally { leases.release(verifyToken); }
    long epoch = quiesce(batch); barrierPreflight(batch, epoch);
    jdbc.update("insert into application_instance_leases(instance_id,build_version,lease_owner,heartbeat_at,lease_expires_at) "
        + "values('late','bad','web',now(),date_add(now(),interval 30 second))");
    String token = leases.acquire(batch, epoch, "EXECUTE");
    try { assertThrows(IllegalStateException.class,
        () -> service.validateContinuation(token, batch, epoch, FamilyCartWalletMigrationRunner.Mode.EXECUTE)); }
    finally { leases.release(token); }
  }

  @Test void cartMergeAndVerifyAreIdempotentAndPartialDdlRecovers() {
    long batch = preflight(); long epoch = quiesce(batch); barrierPreflight(batch, epoch);
    execute(batch, epoch, 101); execute(batch, epoch, 102); complete(batch, epoch);
    String verify = leases.acquire(batch, epoch, "VERIFY");
    try { assertEquals("VERIFIED", service.verify(verify, batch, epoch).status()); }
    finally { leases.release(verify); }
    assertEquals(3L, jdbc.queryForObject("select sum(quantity) from cart_item_selections", Long.class));
    assertEquals("a-new", jdbc.queryForObject(
        "select item_remark from cart_item_selections where user_id=11", String.class));
    jdbc.execute("alter table carts drop index uk_carts_active_cart");
    String finalize = leases.acquire(batch, epoch, "FINALIZE");
    try { service.finalizeBatch(finalize, batch, epoch); }
    finally { leases.release(finalize); }
    assertEquals(1, count("information_schema.statistics where table_schema=database() and table_name='carts' and index_name='uk_carts_active_family'"));
    assertEquals(1, count("information_schema.columns where table_schema=database() and table_name='carts' and column_name='active_family_id'"));
  }

  private long preflight() {
    String token=leases.acquire(0,0,"PREFLIGHT");
    try { return service.preflight(token,null,null).batchId(); } finally { leases.release(token); }
  }
  private long quiesce(long batch) {
    String token=leases.acquire(batch,0,"QUIESCE");
    try { return service.quiesce(token,batch,"test").drainEpoch(); } finally { leases.release(token); }
  }
  private void barrierPreflight(long batch,long epoch) {
    String token=leases.acquire(batch,epoch,"PREFLIGHT");
    try {
      assertEquals("BARRIER_PREFLIGHT", service.preflight(token,batch,epoch).status());
      assertEquals("BARRIER_PREFLIGHT", jdbc.queryForObject(
          "select status from family_wallet_migration_batches where id=?", String.class, batch));
    } finally { leases.release(token); }
  }
  private String acquireAfter(CountDownLatch start) throws InterruptedException {
    start.await();
    return leases.acquire(0,0,"PREFLIGHT");
  }
  private void execute(long batch,long epoch,long family) {
    String token=leases.acquire(batch,epoch,"EXECUTE");
    try { service.executeFamily(token,batch,epoch,family); } finally { leases.release(token); }
  }
  private void complete(long batch,long epoch) {
    String token=leases.acquire(batch,epoch,"EXECUTE");
    try { service.markExecutionComplete(token,batch,epoch); } finally { leases.release(token); }
  }
  private FamilyCartWalletMigrationRunner runner(String mode,Long batch,Long epoch) {
    return new FamilyCartWalletMigrationRunner(service,leases,mode,batch,epoch,
        "runner_family_wallet_disposable","token","token",mockContext());
  }
  private static ConfigurableApplicationContext mockContext() {
    return org.mockito.Mockito.mock(ConfigurableApplicationContext.class);
  }
  private long count(String table) { return jdbc.queryForObject("select count(*) from "+table,Long.class); }
  private BigDecimal money(String sql) { return jdbc.queryForObject(sql,BigDecimal.class); }

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
    jdbc.update("insert into carts(id,merchant_id,family_id,user_id,meal_slot_id,service_date,remark,status,updated_at) values(1001,91,101,11,601,?,'old','active','2026-01-01'),(1002,91,101,11,601,?,'new','active','2026-01-02')",LocalDate.now(),LocalDate.now());
    jdbc.update("insert into cart_items(id,cart_id,dish_id,price,quantity,item_remark,updated_at) values(1101,1001,501,9,1,'a-new','2026-01-03'),(1102,1002,501,9,2,'b-old','2026-01-02')");
  }

  /** Minimal Spring Boot application used only by the disposable MySQL migration tests. */
  @SpringBootApplication @MapperScan("com.familykitchen.migration") @Import(TestConfig.class) static class TestApp { }
  /** Supplies migration services without enabling normal application startup side effects. */
  @TestConfiguration static class TestConfig {
    @Bean FamilyWalletMigrationLeaseService leases(FamilyCartWalletMigrationMapper m){return new FamilyWalletMigrationLeaseService(m);}
    @Bean FamilyWalletMigrationBarrierService barriers(FamilyCartWalletMigrationMapper m,FamilyWalletMigrationLeaseService l){return new FamilyWalletMigrationBarrierService(m,l);}
    @Bean FamilyCartWalletFamilyExecutor executor(FamilyCartWalletMigrationMapper m,FamilyWalletMigrationLeaseService l){return new FamilyCartWalletFamilyExecutor(m,l);}
    @Bean FamilyCartWalletDdlExecutor ddl(FamilyCartWalletMigrationMapper m,FamilyWalletMigrationLeaseService l){return new FamilyCartWalletDdlExecutor(m,l);}
    @Bean FamilyCartWalletMigrationService service(FamilyCartWalletMigrationMapper m,FamilyCartWalletFamilyExecutor e,FamilyWalletMigrationLeaseService l,FamilyWalletMigrationBarrierService b,FamilyCartWalletDdlExecutor d){return new FamilyCartWalletMigrationService(m,e,l,b,d);}
  }
}
