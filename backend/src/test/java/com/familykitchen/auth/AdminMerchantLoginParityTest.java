package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.familykitchen.auth.mapper.AuthContextMapper;
import com.familykitchen.auth.model.dto.AdminLoginRequest;
import com.familykitchen.auth.model.dto.UserLoginRequest;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.auth.service.impl.AdminAuthApplicationServiceImpl;
import com.familykitchen.auth.service.impl.MemberAuthServiceImpl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.model.entity.UserDO;

class AdminMerchantLoginParityTest {
  final UserMapper users = mock(UserMapper.class);
  final AuthContextMapper identities = mock(AuthContextMapper.class);
  final PasswordCodec passwords = mock(PasswordCodec.class);
  final SessionService sessions = mock(SessionService.class);
  final MemberAuthServiceImpl member = new MemberAuthServiceImpl(users, passwords, mock(TokenService.class), null, sessions, identities, null);
  final AdminAuthApplicationServiceImpl admin = new AdminAuthApplicationServiceImpl(member);

  AdminMerchantLoginParityTest() {
    UserDO user = new UserDO(); user.setId(7L); user.setStatus("ACTIVE"); user.setCredentialStatus("ACTIVE"); user.setPasswordHash("hash");
    when(users.findByLoginIdentifier("cook")).thenReturn(user);
    when(passwords.matches("password", "hash")).thenReturn(true);
    when(identities.findPlatformRoles(7L)).thenReturn(List.of());
    when(sessions.create(7L)).thenReturn("session");
  }

  @Test void merchantWithoutPlatformRoleGetsSameIdentityOnBothClients() {
    when(identities.findActiveMerchant(7L)).thenReturn(new AuthContextMapper.MerchantContext(9L, "MERCHANT_ADMIN"));
    var pc = admin.login(new AdminLoginRequest(" Cook ", "password"));
    var mini = member.login(new UserLoginRequest("cook", "password"));
    assertEquals(9L, pc.merchantId());
    assertEquals("merchant_admin", pc.roleTemplate());
    assertEquals(mini, pc);
  }

  @Test void familyMerchantIdDoesNotGrantBackendAccessOrCreateSession() {
    when(identities.findActiveFamily(7L)).thenReturn(new AuthContextMapper.FamilyContext(2L, 9L, "OWNER"));
    assertThrows(BusinessException.class, () -> admin.login(new AdminLoginRequest("cook", "password")));
    verify(sessions, never()).create(anyLong());
    assertEquals("admin", member.login(new UserLoginRequest("cook", "password")).roleTemplate());
  }

  @Test void upperCasePlatformRoleIsAccepted() {
    when(identities.findPlatformRoles(7L)).thenReturn(List.of("PLATFORM_ADMIN"));
    assertEquals("platform_admin", admin.login(new AdminLoginRequest("cook", "password")).roleTemplate());
  }

  @Test void unrelatedRoleDoesNotGrantBackendAccess() {
    when(identities.findPlatformRoles(7L)).thenReturn(List.of("USER"));
    assertThrows(BusinessException.class, () -> admin.login(new AdminLoginRequest("cook", "password")));
    verify(sessions, never()).create(anyLong());
  }

  @Test void inactiveCredentialsAreRejectedOnBothClients() {
    users.findByLoginIdentifier("cook").setCredentialStatus("DISABLED");
    assertThrows(BusinessException.class, () -> admin.login(new AdminLoginRequest("cook", "password")));
    assertThrows(BusinessException.class, () -> member.login(new UserLoginRequest("cook", "password")));
    verify(sessions, never()).create(anyLong());
  }
}
