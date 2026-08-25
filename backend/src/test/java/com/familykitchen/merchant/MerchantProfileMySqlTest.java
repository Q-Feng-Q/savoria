package com.familykitchen.merchant;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.merchant.mapper.MerchantProfileMapper;
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

/** 真实 MySQL 下商户资料更新的负责人关系与商户状态约束测试。 */
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
class MerchantProfileMySqlTest {
  private static final long OWNER_ID = 993001L;
  private static final long MERCHANT_ID = 993001L;
  private static final long OTHER_MERCHANT_ID = 993002L;

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("merchant_profile")
      .withUsername("kitchen_test")
      .withPassword("kitchen_test");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @Autowired private MerchantProfileMapper mapper;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    jdbc.update("delete from merchant_user_relations where merchant_id in (?,?)", MERCHANT_ID, OTHER_MERCHANT_ID);
    jdbc.update("delete from users where id=?", OWNER_ID);
    jdbc.update("delete from merchants where id in (?,?)", MERCHANT_ID, OTHER_MERCHANT_ID);
    jdbc.update("insert into users(id,username,password_hash,nickname,status) values(?,?,?,?, 'ACTIVE')",
        OWNER_ID, "merchant-profile-owner", "test", "负责人");
    jdbc.update("insert into merchants(id,name,status) values(?,?,'active')", MERCHANT_ID, "原商户");
    jdbc.update("insert into merchants(id,name,status) values(?,?,'active')", OTHER_MERCHANT_ID, "其他商户");
    jdbc.update("insert into merchant_user_relations(user_id,merchant_id,merchant_role,status) values(?,?,?,'ACTIVE')",
        OWNER_ID, MERCHANT_ID, "MERCHANT_ADMIN");
  }

  @Test
  void updateRequiresActiveAdminRelationAndNeverTouchesAnotherMerchant() {
    assertThat(mapper.updateProfile(OWNER_ID, MERCHANT_ID, "新商户", "林女士", "13800000000")).isEqualTo(1);
    assertThat(name(MERCHANT_ID)).isEqualTo("新商户");
    assertThat(name(OTHER_MERCHANT_ID)).isEqualTo("其他商户");

    jdbc.update("update merchant_user_relations set status='INACTIVE' where user_id=? and merchant_id=?",
        OWNER_ID, MERCHANT_ID);
    assertThat(mapper.updateProfile(OWNER_ID, MERCHANT_ID, "越权修改", null, null)).isZero();
    assertThat(name(MERCHANT_ID)).isEqualTo("新商户");
  }

  @Test
  void nonAdminRelationAndNonActiveMerchantAreRejected() {
    jdbc.update("update merchant_user_relations set merchant_role='STAFF' where user_id=? and merchant_id=?",
        OWNER_ID, MERCHANT_ID);
    assertThat(mapper.updateProfile(OWNER_ID, MERCHANT_ID, "职员修改", null, null)).isZero();

    jdbc.update("update merchant_user_relations set merchant_role='MERCHANT_ADMIN' where user_id=? and merchant_id=?",
        OWNER_ID, MERCHANT_ID);
    for (String status : new String[] {"pending", "rejected", "inactive"}) {
      jdbc.update("update merchants set status=? where id=?", status, MERCHANT_ID);
      assertThat(mapper.updateProfile(OWNER_ID, MERCHANT_ID, "状态修改", null, null)).isZero();
    }
  }

  private String name(long merchantId) {
    return jdbc.queryForObject("select name from merchants where id=?", String.class, merchantId);
  }
}
