package com.familykitchen;

import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Spring Boot 容器装配测试。
 *
 * <p>该测试用于检查控制器、应用服务、MyBatis Mapper 与基础设施 Bean 是否能被正常扫描和装配。
 * 测试环境使用内存数据库并关闭 Flyway，避免依赖本地 MySQL 可用性。</p>
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:family_kitchen_context;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.flyway.enabled=false",
    "spring.mail.host=localhost",
    "spring.mail.username=test",
    "spring.mail.password=test"
    ,"family-kitchen.wechat.app-id=test-app"
    ,"family-kitchen.wechat.app-secret=test-secret"
    ,"family-kitchen.jwt.secret=test-jwt-secret-with-more-than-32-bytes"
    ,"family-kitchen.auth.bootstrap-admin.password=test-admin-password"
    ,"family-kitchen.account-cancellation.cron=-"
})
class ApplicationContextTest {

  @MockBean
  private UserMapper userMapper;

  @MockBean
  private UserRoleMapper userRoleMapper;

  /**
   * 验证 Spring 容器可以完成启动。
   */
  @Test
  void contextLoads() {
    // Spring Boot 会在进入该方法前完成容器启动；无需额外断言。
  }
}
