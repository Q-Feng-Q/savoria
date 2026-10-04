package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.auth.mapper.UserSessionMapper;
import com.familykitchen.auth.service.impl.SessionServiceImpl;
import com.familykitchen.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Verifies one-time rotating refresh credentials. */
class RenewableSessionServiceTest {
  private final UserSessionMapper mapper = mock(UserSessionMapper.class);
  private final SessionServiceImpl sessions = new SessionServiceImpl(mapper);

  @Test
  void renewableSessionRotatesItsRefreshCredentialAndExtendsIdleWindow() {
    var created = sessions.createRenewable(7L);
    assertEquals(7L, created.userId());
    assertTrue(created.refreshToken().startsWith(created.sessionId() + "."));
    var initialHash = ArgumentCaptor.forClass(String.class);
    verify(mapper).insertRenewable(eq(created.sessionId()), eq(7L), initialHash.capture());
    when(mapper.findRenewableUser(created.sessionId())).thenReturn(7L);
    when(mapper.rotateRefresh(eq(created.sessionId()), eq(7L), eq(initialHash.getValue()), anyString()))
        .thenReturn(1);

    var rotated = sessions.rotateRefresh(created.refreshToken());

    assertEquals(created.sessionId(), rotated.sessionId());
    assertEquals(7L, rotated.userId());
    assertNotEquals(created.refreshToken(), rotated.refreshToken());
    verify(mapper).rotateRefresh(eq(created.sessionId()), eq(7L), eq(initialHash.getValue()), anyString());
  }

  @Test
  void invalidOrReplayedRefreshCredentialCannotBeUsed() {
    assertThrows(BusinessException.class, () -> sessions.rotateRefresh("not-a-refresh-token"));
    var created = sessions.createRenewable(7L);
    when(mapper.findRenewableUser(created.sessionId())).thenReturn(7L);
    when(mapper.rotateRefresh(eq(created.sessionId()), eq(7L), anyString(), anyString())).thenReturn(0);
    assertThrows(BusinessException.class, () -> sessions.rotateRefresh(created.refreshToken()));
  }
}
