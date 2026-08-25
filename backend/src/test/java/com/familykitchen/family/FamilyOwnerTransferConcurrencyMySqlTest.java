package com.familykitchen.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.service.FamilyMemberApplicationService;
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

/** 真实 MySQL 下的负责人并发移交不变量测试。 */
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
class FamilyOwnerTransferConcurrencyMySqlTest {
  private static final long MERCHANT_ID = 992001L;
  private static final long FAMILY_ID = 992001L;
  private static final long OWNER_ID = 992001L;
  private static final long FIRST_ID = 992002L;
  private static final long SECOND_ID = 992003L;
  private static final CurrentUserContext OWNER = new CurrentUserContext(
      OWNER_ID, null, FAMILY_ID, OWNER_ID, "owner", Set.of(), Set.of());

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("family_owner_transfer")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired private FamilyMemberApplicationService families;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void seedFamily() {
    jdbc.update("delete from family_user_relations where family_id=?", FAMILY_ID);
    jdbc.update("delete from users where id in (?,?,?)", OWNER_ID, FIRST_ID, SECOND_ID);
    jdbc.update("delete from families where id=?", FAMILY_ID);
    jdbc.update("delete from merchants where id=?", MERCHANT_ID);
    jdbc.update("insert into merchants(id,name,status) values(?,?,'active')", MERCHANT_ID, "transfer merchant");
    jdbc.update("insert into families(id,merchant_id,name,status) values(?,?,?,'active')",
        FAMILY_ID, MERCHANT_ID, "transfer family");
    for (long userId : List.of(OWNER_ID, FIRST_ID, SECOND_ID)) {
      jdbc.update("insert into users(id,username,password_hash,nickname,status) values(?,?,?,?, 'ACTIVE')",
          userId, "transfer-" + userId, "test", "member-" + userId);
    }
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) values(?,?,?,'ACTIVE','TEST')",
        OWNER_ID, FAMILY_ID, "OWNER");
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) values(?,?,?,'ACTIVE','TEST')",
        FIRST_ID, FAMILY_ID, "MEMBER");
    jdbc.update("insert into family_user_relations(user_id,family_id,family_role,status,join_source) values(?,?,?,'ACTIVE','TEST')",
        SECOND_ID, FAMILY_ID, "MEMBER");
  }

  @Test
  void concurrentTransfersLeaveExactlyOneOwner() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> outcomes = new ArrayList<>();
      outcomes.add(pool.submit(() -> transferAfterBarrier(FIRST_ID, ready, start)));
      outcomes.add(pool.submit(() -> transferAfterBarrier(SECOND_ID, ready, start)));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      int successes = 0;
      for (Future<Boolean> result : outcomes) if (result.get(20, TimeUnit.SECONDS)) successes++;

      assertEquals(1, successes);
      assertEquals(1, jdbc.queryForObject("select count(*) from family_user_relations where family_id=? and status='ACTIVE' and family_role='OWNER'", Integer.class, FAMILY_ID));
      assertEquals("MEMBER", jdbc.queryForObject("select family_role from family_user_relations where family_id=? and user_id=?", String.class, FAMILY_ID, OWNER_ID));
    } finally {
      pool.shutdownNow();
    }
  }

  private boolean transferAfterBarrier(long target, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      if (!start.await(10, TimeUnit.SECONDS)) return false;
      families.transferOwner(OWNER, target);
      return true;
    } catch (Exception expectedConflict) {
      return false;
    }
  }
}
