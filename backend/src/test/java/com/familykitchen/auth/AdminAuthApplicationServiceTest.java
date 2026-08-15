package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.familykitchen.auth.model.dto.AdminLoginRequest;
import com.familykitchen.auth.model.vo.LoginResponse;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.AdminAuthApplicationService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.auth.service.impl.AdminAuthApplicationServiceImpl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.mapper.UserRoleMapper;
import com.familykitchen.user.model.entity.UserDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 验证平台管理Auth申请Service相关业务契约与回归场景。
 */
class AdminAuthApplicationServiceTest {

  @Test
  void loginReturnsAccessTokenForEnabledAdmin() {
    PasswordCodec codec = new PasswordCodec();
    ObjectMapper json = new ObjectMapper();
    AdminAuthApplicationService service = service(buildUser(codec), codec, json);

    LoginResponse response = service.login(new AdminLoginRequest("admin", "123456"));

    assertNotNull(response.accessToken());
    assertEquals(1L, response.userId());
    assertEquals("platform_admin", response.roleTemplate());
    assertEquals(List.of("platform_admin"), response.backendRoles().stream().sorted().toList());
  }

  @Test
  void loginRejectsWrongPassword() {
    PasswordCodec codec = new PasswordCodec();
    ObjectMapper json = new ObjectMapper();
    AdminAuthApplicationService service = service(buildUser(codec), codec, json);
    assertThrows(BusinessException.class,
        () -> service.login(new AdminLoginRequest("admin", "wrong-password")));
  }

  private static AdminAuthApplicationService service(UserDO user, PasswordCodec codec,
      ObjectMapper json) {
    UserMapper users = proxy(UserMapper.class, (method, args) ->
        "findByLoginIdentifier".equals(method) ? user : null);
    UserRoleMapper roles = proxy(UserRoleMapper.class, (method, args) ->
        "findActiveRoles".equals(method) ? List.of("platform_admin") : null);
    SessionService sessions = new SessionService() {
      public String create(Long userId) { return "test-session"; }
      public void requireActive(Long userId, String sessionId) { }
      public void revoke(Long userId, String sessionId) { }
      public void revokeAll(Long userId) { }
    };
    return new AdminAuthApplicationServiceImpl(users, roles, codec,
        new TokenService(json, "unit-test-secret", 7200L), sessions);
  }

  private static UserDO buildUser(PasswordCodec codec) {
    UserDO user = new UserDO();
    user.setId(1L);
    user.setUsername("admin");
    user.setNickname("System Administrator");
    user.setPasswordHash(codec.encode("123456"));
    user.setStatus("ACTIVE");
    return user;
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, Invocation invocation) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
        (instance, method, args) -> {
          if ("hashCode".equals(method.getName())) return System.identityHashCode(instance);
          if ("equals".equals(method.getName())) return instance == args[0];
          if ("toString".equals(method.getName())) return type.getSimpleName() + "TestProxy";
          return invocation.call(method.getName(), args);
        });
  }

  /**
   * 验证Invocation相关业务契约与回归场景。
   */
  private interface Invocation {
    /**
     * 处理Invocation。
     *
     * @param method method
     * @param args args
     * @return 处理结果后的结果
     */
    Object call(String method, Object[] args);
  }
}
