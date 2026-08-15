package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.auth.security.PasswordCodec;
import org.junit.jupiter.api.Test;

/**
 * 验证密码Codec相关业务契约与回归场景。
 */
class PasswordCodecTest {

  @Test
  void encodedPasswordCanBeVerified() {
    PasswordCodec passwordCodec = new PasswordCodec();

    String encoded = passwordCodec.encode("Kitchen@2026");

    assertTrue(passwordCodec.matches("Kitchen@2026", encoded));
    assertFalse(passwordCodec.matches("wrong-password", encoded));
  }
}

