package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.auth.mapper.AuthContextMapper;
import com.familykitchen.auth.model.dto.UserLoginRequest;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.auth.service.impl.MemberAuthServiceImpl;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.model.entity.UserDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies mini-program renewal without extending admin sessions. */
class MemberSessionRenewalTest {
  private final UserMapper users = mock(UserMapper.class);
  private final SessionService sessions = mock(SessionService.class);
  private final AuthContextMapper identities = mock(AuthContextMapper.class);
  private final PasswordCodec passwords = mock(PasswordCodec.class);
  private final MemberAuthServiceImpl auth = new MemberAuthServiceImpl(users, passwords,
      new TokenService(new ObjectMapper(), "unit-test-secret", 7200L), null, sessions, identities, null);

  @Test
  void miniLoginReceivesRefreshCredentialAndAdminLoginDoesNot() {
    UserDO user = activeUser();
    user.setPasswordHash("hash");
    when(users.findByLoginIdentifier("cook")).thenReturn(user);
    when(passwords.matches("password", "hash")).thenReturn(true);
    when(identities.findPlatformRoles(7L)).thenReturn(List.of("platform_admin"));
    when(sessions.createRenewable(7L)).thenReturn(
        new SessionService.RenewableSession(7L, "mini-session", "mini-refresh"));
    when(sessions.create(7L)).thenReturn("admin-session");

    var mini = auth.login(new UserLoginRequest("cook", "password"));
    var admin = auth.loginAdmin(new UserLoginRequest("cook", "password"));

    assertEquals("mini-refresh", mini.refreshToken());
    assertNotNull(mini.accessToken());
    assertNull(admin.refreshToken());
  }

  @Test
  void refreshIssuesAccessTokenForTheSameAccountWithoutCreatingAnotherSession() {
    when(sessions.rotateRefresh("old-refresh")).thenReturn(
        new SessionService.RenewableSession(7L, "same-session", "new-refresh"));
    when(users.findById(7L)).thenReturn(activeUser());
    when(identities.findPlatformRoles(7L)).thenReturn(List.of());

    var refreshed = auth.refresh("old-refresh");

    assertEquals(7L, refreshed.userId());
    assertEquals("new-refresh", refreshed.refreshToken());
    assertEquals("same-session", new TokenService(new ObjectMapper(), "unit-test-secret", 7200L)
        .parse(refreshed.accessToken()).sessionId());
  }

  private static UserDO activeUser() {
    UserDO user = new UserDO();
    user.setId(7L);
    user.setStatus("ACTIVE");
    user.setCredentialStatus("ACTIVE");
    return user;
  }
}
