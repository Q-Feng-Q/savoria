package com.familykitchen.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.cart.service.ActiveCartMemberCleanupService;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies exit-versus-order-snapshot serialization on disposable MySQL 8. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "family-kitchen.instance.lease-enabled=false", "family-kitchen.migration.mode=OFF",
    "spring.mail.host=localhost", "spring.mail.username=test", "spring.mail.password=test",
    "family-kitchen.wechat.app-id=test", "family-kitchen.wechat.app-secret=test",
    "family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes",
    "family-kitchen.auth.bootstrap-admin.password=test-admin-password",
    "family-kitchen.account-cancellation.cron=-"
})
class FamilyMemberCartCleanupConcurrencyMySqlTest {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("member_cart_cleanup")
      .withUsername("cleanup_test")
      .withPassword("cleanup_test");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired
  ActiveCartMemberCleanupService cleanup;
  @Autowired
  CartMapper carts;
  @Autowired
  JdbcTemplate jdbc;
  @Autowired
  PlatformTransactionManager transactions;

  @BeforeEach
  void reset() {
    jdbc.update("delete cis from cart_item_selections cis join cart_items ci on ci.id=cis.cart_item_id "
        + "join carts c on c.id=ci.cart_id where c.family_id=9301");
    jdbc.update("delete ci from cart_items ci join carts c on c.id=ci.cart_id where c.family_id=9301");
    jdbc.update("delete from carts where family_id=9301");
    jdbc.update("delete from family_user_relations where family_id=9301");
    jdbc.update("delete from families where id=9301");
    jdbc.update("delete from users where id in (9311,9312)");
    jdbc.update("delete from merchants where id=9301");
    jdbc.update("insert into merchants(id,name,status) values(9301,'退出竞态私厨','active')");
    jdbc.update("insert into users(id,username,password_hash,nickname) values"
        + "(9311,'cleanup-race-1','x','小林'),(9312,'cleanup-race-2','x','阿禾')");
    jdbc.update("insert into families(id,merchant_id,name,status) values(9301,9301,'退出竞态家庭','active')");
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) "
        + "values(9311,9301,'MEMBER','ACTIVE','TEST'),(9312,9301,'OWNER','ACTIVE','TEST')");
    jdbc.update("insert into carts(id,merchant_id,family_id,status,version) "
        + "values(9341,9301,9301,'active',0)");
    jdbc.update("insert into cart_items(id,cart_id,dish_id,dish_name_snapshot,price,quantity) "
        + "values(9351,9341,99,'番茄牛腩',38,3)");
    jdbc.update("insert into cart_item_selections(cart_item_id,user_id,quantity) "
        + "values(9351,9311,1),(9351,9312,2)");
  }

  @Test
  void exitAndSnapshotNeverProduceAttributionForAnAlreadyInvalidMember() throws Exception {
    List<Boolean> outcomes = race(
        () -> transaction(() -> {
          cleanup.removeMember(9301L, 9311L);
          jdbc.update("update family_user_relations set status='EXITED' "
              + "where family_id=9301 and user_id=9311 and status='ACTIVE'");
          return true;
        }),
        () -> transaction(this::snapshotIsMembershipConsistent));

    assertTrue(outcomes.stream().allMatch(Boolean::booleanValue));
    assertEquals(0, jdbc.queryForObject(
        "select count(*) from cart_item_selections where user_id=9311", Integer.class));
  }

  private boolean snapshotIsMembershipConsistent() {
    carts.selectFamilyActiveCartForUpdate(9301L);
    List<CartItemEntity> items = carts.selectCartItemsForUpdate(9341L);
    List<CartItemSelectionEntity> selections = new ArrayList<>();
    for (CartItemEntity item : items) {
      selections.addAll(carts.selectSelectionsForUpdate(item.getId()));
    }
    List<Long> activeMembers = jdbc.queryForList(
        "select user_id from family_user_relations where family_id=9301 "
            + "and status='ACTIVE' order by user_id for update", Long.class);
    boolean hasExitedSelection = selections.stream().anyMatch(row -> row.userId.equals(9311L));
    return !hasExitedSelection || activeMembers.contains(9311L);
  }

  private <T> T transaction(java.util.concurrent.Callable<T> action) {
    return new TransactionTemplate(transactions).execute(status -> {
      try {
        return action.call();
      } catch (Exception exception) {
        throw new IllegalStateException(exception);
      }
    });
  }

  private static List<Boolean> race(
      java.util.concurrent.Callable<Boolean> first, java.util.concurrent.Callable<Boolean> second)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> futures = List.of(
          pool.submit(() -> run(first, ready, start)),
          pool.submit(() -> run(second, ready, start)));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      List<Boolean> results = new ArrayList<>();
      for (Future<Boolean> future : futures) {
        results.add(future.get(20, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }

  private static boolean run(
      java.util.concurrent.Callable<Boolean> action, CountDownLatch ready, CountDownLatch start)
      throws Exception {
    ready.countDown();
    start.await(10, TimeUnit.SECONDS);
    return action.call();
  }
}
