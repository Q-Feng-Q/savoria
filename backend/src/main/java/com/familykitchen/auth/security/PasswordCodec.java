package com.familykitchen.auth.security;

import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 用户密码编解码组件。
 *
 * <p>新密码统一使用 BCrypt；迁移期间兼容旧 PBKDF2 摘要，登录成功后由服务升级。</p>
 */
@Component
public class PasswordCodec {
  private static final String LEGACY_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int LEGACY_KEY_LENGTH = 256;
  private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

  /** 使用 BCrypt 编码新密码。 
   * @param rawPassword raw密码
   * @return 处理结果
   */
  public String encode(String rawPassword) {
    return bcrypt.encode(rawPassword);
  }

  /** 根据摘要格式验证 BCrypt 或历史 PBKDF2 密码。 
   * @param rawPassword raw密码
   * @param encodedPassword encoded密码
   * @return 是否满足对应条件
   */
  public boolean matches(String rawPassword, String encodedPassword) {
    if (encodedPassword == null) {
      return false;
    }
    if (encodedPassword.startsWith("$2")) {
      return bcrypt.matches(rawPassword, encodedPassword);
    }
    return matchesLegacyPbkdf2(rawPassword, encodedPassword);
  }

  /** 判断摘要是否需要升级为 BCrypt。 
   * @param encodedPassword encoded密码
   * @return 是否满足对应条件
   */
  public boolean requiresUpgrade(String encodedPassword) {
    return encodedPassword != null && !encodedPassword.startsWith("$2");
  }

  private boolean matchesLegacyPbkdf2(String rawPassword, String encodedPassword) {
    try {
      String[] parts = encodedPassword.split("\\$");
      if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
        return false;
      }
      int iterations = Integer.parseInt(parts[1]);
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expected = Base64.getDecoder().decode(parts[3]);
      SecretKeyFactory factory = SecretKeyFactory.getInstance(LEGACY_ALGORITHM);
      KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, LEGACY_KEY_LENGTH);
      byte[] actual = factory.generateSecret(spec).getEncoded();
      if (expected.length != actual.length) {
        return false;
      }
      int difference = 0;
      for (int index = 0; index < expected.length; index++) {
        difference |= expected[index] ^ actual[index];
      }
      return difference == 0;
    } catch (Exception ignored) {
      return false;
    }
  }
}
