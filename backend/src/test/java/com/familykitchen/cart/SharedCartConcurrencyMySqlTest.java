package com.familykitchen.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.cart.mapper.CartMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.cart.model.dto.CartMutationRequest;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.cart.service.CartApplicationService;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.cart.service.impl.CartApplicationServiceImpl;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
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

/** Exercises shared-cart creation, CAS, replay, and submission races on disposable MySQL 8. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "family-kitchen.instance.lease-enabled=false", "family-kitchen.migration.mode=OFF",
    "spring.mail.host=localhost", "spring.mail.username=test", "spring.mail.password=test",
    "family-kitchen.wechat.app-id=test", "family-kitchen.wechat.app-secret=test",
    "family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes",
    "family-kitchen.auth.bootstrap-admin.password=test-admin-password",
    "family-kitchen.account-cancellation.cron=-"
})
class SharedCartConcurrencyMySqlTest {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("shared_cart_concurrency")
      .withUsername("cart_test")
      .withPassword("cart_test");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired
  CartApplicationService carts;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  CartMapper cartMapper;
  @Autowired
  ExpectedMealTimePolicy timePolicy;
  @Autowired
  CommandIdempotencyService commands;
  @Autowired
  ObjectMapper objectMapper;

  private final CurrentUserContext first = user(9101L);
  private final CurrentUserContext second = user(9102L);

  @BeforeEach
  void reset() {
    finalizeActiveCartKey();
    jdbc.update("delete from command_idempotency where family_id=9001");
    jdbc.update("delete cis from cart_item_selections cis join cart_items ci on ci.id=cis.cart_item_id "
        + "join carts c on c.id=ci.cart_id where c.family_id=9001");
    jdbc.update("delete ci from cart_items ci join carts c on c.id=ci.cart_id where c.family_id=9001");
    jdbc.update("delete from carts where family_id=9001");
    jdbc.update("delete from family_menu_items where family_id=9001");
    jdbc.update("delete from dishes where id=9201");
    jdbc.update("delete from dish_categories where id=9200");
    jdbc.update("delete from family_user_relations where family_id=9001");
    jdbc.update("delete from families where id=9001");
    jdbc.update("delete from users where id in (9101,9102)");
    jdbc.update("delete from merchants where id=9001");
    jdbc.update("insert into merchants(id,name,status) values(9001,'并发私厨','active')");
    jdbc.update("insert into users(id,username,password_hash,nickname) values"
        + "(9101,'cart-race-1','x','小林'),(9102,'cart-race-2','x','阿禾')");
    jdbc.update("insert into families(id,merchant_id,name,status) values(9001,9001,'并发家庭','active')");
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) "
        + "values(9101,9001,'OWNER','ACTIVE','TEST'),(9102,9001,'MEMBER','ACTIVE','TEST')");
    jdbc.update("insert into dish_categories(id,merchant_id,name) values(9200,9001,'测试分类')");
    jdbc.update("insert into dishes(id,merchant_id,category_id,name,base_price,status) "
        + "values(9201,9001,9200,'番茄牛腩',38,'active')");
    jdbc.update("insert into family_menu_items(family_id,dish_id,enabled,final_price) "
        + "values(9001,9201,1,36)");
  }

  @Test
  void concurrentFirstGetCreatesExactlyOneActiveCart() throws Exception {
    List<CartView> views = race(() -> carts.cart(first), () -> carts.cart(second));
    assertEquals(2, views.size());
    assertEquals(views.get(0).cartId(), views.get(1).cartId());
    assertEquals(1, count("select count(*) from carts where family_id=9001 and status='active'"));
  }

  @Test
  void concurrentAbsoluteMutationsHaveOneWinnerAndOneStableVersionConflict() throws Exception {
    CartView cart = carts.cart(first);
    List<Object> outcomes = raceOutcome(
        () -> carts.mutateItem(first,
            new CartMutationRequest(cart.cartId(), 0L, "race-item-1", 9201L, 1, null)),
        () -> carts.mutateItem(second,
            new CartMutationRequest(cart.cartId(), 0L, "race-item-2", 9201L, 2, null)));
    assertEquals(1, outcomes.stream().filter(CartView.class::isInstance).count());
    assertEquals(1, outcomes.stream().filter(this::isCartChanged).count());
    assertEquals(1, count("select count(*) from cart_item_selections"));
  }

  @Test
  void persistentRequestReplayReturnsTheStoredResponseAndMutatesOnce() {
    CartView cart = carts.cart(first);
    CartMutationRequest request =
        new CartMutationRequest(cart.cartId(), 0L, "durable-replay", 9201L, 1, null);
    CartView original = carts.mutateItem(first, request);
    jdbc.update("update family_menu_items set final_price=34 where family_id=9001 and dish_id=9201");
    CartView changed = carts.mutateItem(first,
        new CartMutationRequest(cart.cartId(), 1L, "later-change", 9201L, 2, null));
    CartApplicationService restarted =
        new CartApplicationServiceImpl(cartMapper, timePolicy, commands, objectMapper);
    CartView replay = restarted.mutateItem(first, request);
    assertEquals(original, replay);
    assertEquals(1, original.version());
    assertEquals(2, changed.version());
    assertEquals(2, count("select version from carts where id=" + cart.cartId()));
    assertEquals(2, count("select quantity from cart_item_selections where user_id=9101"));
    assertEquals(new BigDecimal("34.00"), jdbc.queryForObject(
        "select price from cart_items where cart_id=?", BigDecimal.class, cart.cartId()));
  }

  @Test
  void submitVersusWriteNeverMovesTheStaleWriteIntoANextCart() throws Exception {
    CartView cart = carts.cart(first);
    List<Object> outcomes = raceOutcome(
        () -> carts.mutateItem(first,
            new CartMutationRequest(cart.cartId(), 0L, "submit-race-write", 9201L, 1, null)),
        () -> jdbc.update("update carts set status='submitted' "
            + "where id=? and status='active' and version=0", cart.cartId()));
    long writeWins = outcomes.stream().filter(CartView.class::isInstance).count();
    long submitWins = outcomes.stream()
        .filter(value -> value instanceof Integer affected && affected == 1).count();
    assertEquals(1, writeWins + submitWins);
    if (submitWins == 1) {
      assertTrue(outcomes.stream().anyMatch(this::isCartSubmitted));
      assertEquals(0, count("select count(*) from cart_item_selections"));
    }
  }

  private void finalizeActiveCartKey() {
    if (count("select count(*) from information_schema.statistics where table_schema=database() "
        + "and table_name='carts' and index_name='uk_carts_active_cart'") == 1) {
      jdbc.execute("alter table carts drop index uk_carts_active_cart");
    }
    if (count("select count(*) from information_schema.columns where table_schema=database() "
        + "and table_name='carts' and column_name='active_cart_key'") == 1) {
      jdbc.execute("alter table carts drop column active_cart_key");
    }
    if (count("select count(*) from information_schema.columns where table_schema=database() "
        + "and table_name='carts' and column_name='active_family_id'") == 0) {
      jdbc.execute("alter table carts add column active_family_id bigint generated always as "
          + "(case when status='active' then family_id else null end) stored");
    }
    if (count("select count(*) from information_schema.statistics where table_schema=database() "
        + "and table_name='carts' and index_name='uk_carts_active_family'") == 0) {
      jdbc.execute("alter table carts add unique key uk_carts_active_family(active_family_id)");
    }
  }

  private boolean isCartChanged(Object value) {
    return value instanceof BusinessException failure
        && failure.errorCode() == ErrorCode.CART_CHANGED;
  }

  private boolean isCartSubmitted(Object value) {
    return value instanceof BusinessException failure
        && failure.errorCode() == ErrorCode.CART_SUBMITTED;
  }

  private int count(String sql) {
    return jdbc.queryForObject(sql, Integer.class);
  }

  private static CurrentUserContext user(Long userId) {
    return new CurrentUserContext(
        userId, 9001L, 9001L, userId, "member", Set.of(), Set.of());
  }

  private static <T> List<T> race(
      java.util.concurrent.Callable<T> first, java.util.concurrent.Callable<T> second)
      throws Exception {
    List<Object> values = raceOutcome(first, second);
    List<T> typed = new ArrayList<>();
    for (Object value : values) {
      if (value instanceof Throwable failure) {
        throw new AssertionError(failure);
      }
      @SuppressWarnings("unchecked")
      T cast = (T) value;
      typed.add(cast);
    }
    return typed;
  }

  private static List<Object> raceOutcome(
      java.util.concurrent.Callable<?> first, java.util.concurrent.Callable<?> second)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Object>> futures = List.of(
          pool.submit(() -> run(first, ready, start)),
          pool.submit(() -> run(second, ready, start)));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      List<Object> results = new ArrayList<>();
      for (Future<Object> future : futures) {
        results.add(future.get(20, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }

  private static Object run(
      java.util.concurrent.Callable<?> action, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      start.await(10, TimeUnit.SECONDS);
      return action.call();
    } catch (Throwable failure) {
      return failure;
    }
  }
}
