package com.familykitchen.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.system.security.PlatformSecretCipher;
import org.junit.jupiter.api.Test;

/** 验证平台敏感配置密文可正确还原且能检测篡改，防止 SMTP 密码以明文落库。 */
class PlatformSecretCipherTest {
  @Test
  void encryptsAndDecryptsSmtpPasswordWithoutStoringPlaintext() {
    var cipher = new PlatformSecretCipher("test-platform-secret-with-enough-entropy");
    String encrypted = cipher.encrypt("mail-authorization-code");
    assertThat(encrypted).startsWith("v1:").doesNotContain("mail-authorization-code");
    assertThat(cipher.decrypt(encrypted)).isEqualTo("mail-authorization-code");
  }

  @Test
  void rejectsTamperedCiphertext() {
    var cipher = new PlatformSecretCipher("test-platform-secret-with-enough-entropy");
    assertThatThrownBy(() -> cipher.decrypt(cipher.encrypt("secret") + "broken"))
        .isInstanceOf(IllegalStateException.class);
  }
}
