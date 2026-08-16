package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.dto.DishStatusRequest;
import com.familykitchen.dish.service.DishApplicationService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

/** Real-MySQL concurrency checks for merchant-wide recommendation invariants. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "spring.mail.host=localhost",
    "spring.mail.username=test",
    "spring.mail.password=test",
    "family-kitchen.wechat.app-id=test-app",
    "family-kitchen.wechat.app-secret=test-secret",
    "family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes",
    "family-kitchen.auth.bootstrap-admin.password=test-admin-password",
    "family-kitchen.account-cancellation.cron=-"
})
class MerchantFeaturedDishConcurrencyMySqlTest {
  private static final long MERCHANT_ID = 990001L;
  private static final long CATEGORY_ID = 990001L;
  private static final long FAMILY_ID = 990001L;
  private static final CurrentUserContext MERCHANT = new CurrentUserContext(
      990001L, MERCHANT_ID, null, null, "owner", Set.of(), Set.of("MERCHANT_ADMIN"));

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_kitchen_featured_concurrency")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired private DishApplicationService dishes;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void seedMerchant() {
    jdbc.update("delete from family_menu_items where family_id=?", FAMILY_ID);
    jdbc.update("delete from dishes where merchant_id=?", MERCHANT_ID);
    jdbc.update("delete from families where merchant_id=?", MERCHANT_ID);
    jdbc.update("delete from dish_categories where merchant_id=?", MERCHANT_ID);
    jdbc.update("delete from merchants where id=?", MERCHANT_ID);
    jdbc.update("insert into merchants(id,name,status) values(?,?,'active')", MERCHANT_ID, "race merchant");
    jdbc.update("insert into dish_categories(id,merchant_id,name) values(?,?,?)",
        CATEGORY_ID, MERCHANT_ID, "race category");
    jdbc.update("insert into families(id,merchant_id,name,status) values(?,?,?,'active')",
        FAMILY_ID, MERCHANT_ID, "race family");
    for (long dishId = 990101L; dishId <= 990107L; dishId++) {
      jdbc.update("insert into dishes(id,merchant_id,category_id,name,base_price,status) "
              + "values(?,?,?,?,?,'active')", dishId, MERCHANT_ID, CATEGORY_ID,
          "dish-" + dishId, BigDecimal.valueOf(dishId % 100));
    }
  }

  @Test
  void sixConcurrentRecommendationsNeverExceedFive() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(6);
    CountDownLatch ready = new CountDownLatch(6);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> outcomes = new ArrayList<>();
      for (long dishId = 990101L; dishId <= 990106L; dishId++) {
        long candidate = dishId;
        outcomes.add(pool.submit(() -> recommendAfterBarrier(candidate, ready, start)));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      long successes = 0;
      for (Future<Boolean> outcome : outcomes) if (outcome.get(20, TimeUnit.SECONDS)) successes++;

      assertEquals(5L, successes);
      assertEquals(5, jdbc.queryForObject("select count(*) from dishes "
          + "where merchant_id=? and status='active' and featured_at is not null", Integer.class, MERCHANT_ID));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void recommendationRacingInactiveCanNeverLeaveInactiveFeaturedDish() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<?> recommend = pool.submit(() -> {
        awaitBarrier(ready, start);
        try { dishes.setFeaturedDish(MERCHANT, 990107L, true); } catch (BusinessException ignored) { }
      });
      Future<?> inactive = pool.submit(() -> {
        awaitBarrier(ready, start);
        dishes.updateDishStatus(MERCHANT, 990107L, new DishStatusRequest("INACTIVE"));
      });
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      recommend.get(20, TimeUnit.SECONDS);
      inactive.get(20, TimeUnit.SECONDS);

      assertEquals("inactive", jdbc.queryForObject(
          "select status from dishes where id=?", String.class, 990107L));
      assertNull(jdbc.queryForObject(
          "select featured_at from dishes where id=?", Object.class, 990107L));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void recommendationAutoEnablesFamiliesAndPreservesExistingOverrides() {
    jdbc.update("insert into family_menu_items(family_id,dish_id,enabled,sort_order,final_price) "
        + "values(?,?,0,7,99.00)", FAMILY_ID, 990101L);

    dishes.setFeaturedDish(MERCHANT, 990101L, true);

    assertEquals(Boolean.TRUE, jdbc.queryForObject("select enabled from family_menu_items "
        + "where family_id=? and dish_id=?", Boolean.class, FAMILY_ID, 990101L));
    assertEquals(7, jdbc.queryForObject("select sort_order from family_menu_items "
        + "where family_id=? and dish_id=?", Integer.class, FAMILY_ID, 990101L));
    assertEquals(new BigDecimal("99.00"), jdbc.queryForObject("select final_price from family_menu_items "
        + "where family_id=? and dish_id=?", BigDecimal.class, FAMILY_ID, 990101L));
  }

  private boolean recommendAfterBarrier(long dishId, CountDownLatch ready, CountDownLatch start) {
    awaitBarrier(ready, start);
    try {
      dishes.setFeaturedDish(MERCHANT, dishId, true);
      return true;
    } catch (BusinessException expectedLimit) {
      return false;
    }
  }

  private static void awaitBarrier(CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("barrier timed out");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interrupted);
    }
  }
}
