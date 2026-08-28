package com.familykitchen.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.order.service.FamilyOrderApplicationService;
import com.familykitchen.order.service.MerchantOrderApplicationService;
import com.familykitchen.wallet.service.FamilyWalletService;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exercises order/hold/account lock ordering on a disposable MySQL database. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "family-kitchen.instance.lease-enabled=false", "family-kitchen.migration.mode=OFF",
    "spring.mail.host=localhost", "spring.mail.username=test", "spring.mail.password=test",
    "family-kitchen.wechat.app-id=test", "family-kitchen.wechat.app-secret=test",
    "family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes",
    "family-kitchen.auth.bootstrap-admin.password=test-admin-password",
    "family-kitchen.account-cancellation.cron=-"
})
class FamilyWalletOrderLifecycleMySqlTest {
  @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_order_lifecycle").withUsername("lifecycle_test")
      .withPassword("lifecycle_test");

  @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired MerchantOrderApplicationService merchantOrders;
  @Autowired FamilyOrderApplicationService familyOrders;
  @Autowired FamilyWalletService wallets;
  @Autowired JdbcTemplate jdbc;
  private final CurrentUserContext merchant = new CurrentUserContext(
      9702L, 9701L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());
  private final CurrentUserContext family = new CurrentUserContext(
      9703L, 9701L, 9701L, 9703L, "owner", Set.of(), Set.of());

  @BeforeEach void reset() {
    jdbc.update("delete from notifications where receiver_id in (9701,9703)");
    jdbc.update("delete from family_wallet_ledgers where family_id=9701");
    jdbc.update("delete from family_wallet_order_holds where family_id=9701");
    jdbc.update("delete from orders where id=9710");
    jdbc.update("delete from family_wallets where family_id=9701");
    jdbc.update("delete from family_user_relations where family_id=9701");
    jdbc.update("delete from families where id=9701");
    jdbc.update("delete from users where id in (9702,9703)");
    jdbc.update("delete from merchants where id=9701");
    jdbc.update("insert into merchants(id,name,status) values(?,?,?)", 9701, "生命周期私厨", "active");
    jdbc.update("insert into users(id,username,password_hash,nickname) values(?,?,?,?),(?,?,?,?)",
        9702, "lifecycle-merchant", "x", "商户", 9703, "lifecycle-family", "x", "小林");
    jdbc.update("insert into families(id,merchant_id,name,status,delivery_enabled,"
        + "delivery_fee_default,delivery_free) values(?,?,?,?,?,?,?)",
        9701, 9701, "生命周期家庭", "active", 1, 0, 0);
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) "
        + "values(?,?,?,?,?)", 9703, 9701, "OWNER", "ACTIVE", "TEST");
    jdbc.update("insert into family_wallets(family_id,available_amount,frozen_amount) values(?,?,?)",
        9701, new BigDecimal("101.00"), BigDecimal.ZERO);
    jdbc.update("insert into orders(id,merchant_id,family_id,submitter_user_id,service_date,"
        + "expected_meal_time,delivery_mode,delivery_fee,status,total_amount) "
        + "values(?,?,?,?,current_date,date_add(now(),interval 2 hour),?,?,?,?)",
        9710, 9701, 9701, 9703, "DELIVERY", BigDecimal.ZERO, "PENDING",
        new BigDecimal("100.00"));
    wallets.freezeNewOrder(9701, 9710, 9703, new BigDecimal("100.00"), "initial");
  }

  @Test void feeAdjustmentVersusCancelKeepsAccountAndHoldConserved() throws Exception {
    RaceResult result = race(
        () -> merchantOrders.adjustDeliveryFee(
            merchant, 9710L, new BigDecimal("1.00"), "race-fee"),
        () -> familyOrders.cancel(family, 9710L, "改变计划"));
    assertTrue(result.successes() >= 1);
    assertNoDeadlock(result);
    assertEquals("CANCELLED", jdbc.queryForObject(
        "select status from orders where id=9710", String.class));
    assertEquals("RELEASED", jdbc.queryForObject(
        "select status from family_wallet_order_holds where order_id=9710", String.class));
    assertEquals(new BigDecimal("101.00"), money("available_amount"));
    assertEquals(new BigDecimal("0.00"), money("frozen_amount"));
  }

  @Test void insufficientFeeIncreaseRollsBackOrderAndWallet() {
    org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
        () -> merchantOrders.adjustDeliveryFee(
            merchant, 9710L, new BigDecimal("2.00"), "too-large"));
    assertEquals(new BigDecimal("0.00"), jdbc.queryForObject(
        "select delivery_fee from orders where id=9710", BigDecimal.class));
    assertEquals(new BigDecimal("1.00"), money("available_amount"));
    assertEquals(new BigDecimal("100.00"), money("frozen_amount"));
  }

  @Test void duplicateRejectReleasesOnlyOnce() {
    merchantOrders.reject(merchant, 9710L);
    merchantOrders.reject(merchant, 9710L);
    assertEquals(new BigDecimal("101.00"), money("available_amount"));
    assertEquals(2, jdbc.queryForObject(
        "select count(*) from family_wallet_ledgers where family_id=9701", Integer.class));
  }

  @Test void settledRefundCreditsAvailableAndTracksRefundedAmount() {
    wallets.capture(9701, 9710, 9702, new BigDecimal("100.00"), "capture");
    wallets.refund(9701, 9710, 9702, new BigDecimal("25.00"), "refund-25");
    assertEquals(new BigDecimal("26.00"), money("available_amount"));
    assertEquals(new BigDecimal("25.00"), jdbc.queryForObject(
        "select refunded_amount from family_wallet_order_holds where order_id=9710",
        BigDecimal.class));
  }

  private BigDecimal money(String column) {
    return jdbc.queryForObject(
        "select " + column + " from family_wallets where family_id=9701", BigDecimal.class);
  }

  private static RaceResult race(ThrowingAction first, ThrowingAction second) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<Throwable> one = pool.submit(() -> run(first, ready, start));
      Future<Throwable> two = pool.submit(() -> run(second, ready, start));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      Throwable firstFailure = one.get(30, TimeUnit.SECONDS);
      Throwable secondFailure = two.get(30, TimeUnit.SECONDS);
      int successes = (firstFailure == null ? 1 : 0) + (secondFailure == null ? 1 : 0);
      return new RaceResult(successes, firstFailure, secondFailure);
    } finally {
      pool.shutdownNow();
    }
  }

  private static Throwable run(
      ThrowingAction action, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      start.await(10, TimeUnit.SECONDS);
      action.run();
      return null;
    } catch (Throwable failure) {
      return failure;
    }
  }

  private static void assertNoDeadlock(RaceResult result) {
    for (Throwable failure : new Throwable[]{result.firstFailure(), result.secondFailure()}) {
      if (failure == null) continue;
      String message = String.valueOf(failure.getMessage()).toLowerCase();
      assertTrue(!message.contains("deadlock") && !message.contains("40001"),
          "race must not deadlock: " + failure);
    }
  }

  /** One concurrent lifecycle operation. */
  @FunctionalInterface interface ThrowingAction {
    /** Runs the operation.
     * @throws Exception operation failure
     */
    void run() throws Exception;
  }

  /** Captures both race outcomes.
   * @param successes successful operations
   * @param firstFailure first failure, if any
   * @param secondFailure second failure, if any
   */
  record RaceResult(int successes, Throwable firstFailure, Throwable secondFailure) {}
}
