package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.familykitchen.auth.security.TokenService;
import com.familykitchen.common.security.CurrentUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 验证令牌Service相关业务契约与回归场景。
 */
class TokenServiceTest {

  @Test
  void issueAndParseRoundTrip() {
    TokenService tokenService = new TokenService(new ObjectMapper(), "unit-test-secret", 7200L);
    CurrentUserContext source = new CurrentUserContext(
        99L,
        9L,
        2L,
        10L,
        "merchant_admin",
        Set.of("merchant_admin"),
        Set.of("merchant"),
        "session-1"
    );

    String token = tokenService.issue(source);
    CurrentUserContext parsed = tokenService.parse(token);

    assertEquals(source.userId(), parsed.userId());
    assertEquals("session-1", parsed.sessionId());
    assertEquals(null, parsed.merchantId());
  }
}

