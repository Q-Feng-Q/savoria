package com.familykitchen.system.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 使用 AES-GCM 加密平台敏感配置，当前用于保护持久化的 SMTP 密码。 */
@Component
public class PlatformSecretCipher {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final SecretKeySpec key;

  /**
   * 由平台配置密钥派生固定长度 AES 密钥。
   * @param secret 平台配置密钥；默认复用 JWT 密钥配置
   */
  public PlatformSecretCipher(@Value("${family-kitchen.platform-config-secret:${family-kitchen.jwt.secret}}") String secret) {
    try { this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)), "AES"); }
    catch (Exception e) { throw new IllegalStateException("Unable to initialize platform secret cipher", e); }
  }

  /**
   * 以随机 nonce 加密明文，并添加版本前缀便于后续轮换算法。
   * <p>AES-GCM 同时提供保密性与篡改检测；每次随机 nonce 可避免相同密码产生相同密文。</p>
   * @param plaintext 待加密明文，空白值直接转换为空
   * @return {@code v1:} 前缀的 Base64 密文，输入为空时返回空
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) return null;
    try {
      byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
    } catch (Exception e) { throw new IllegalStateException("平台敏感配置加密失败", e); }
  }

  /**
   * 解密当前版本的平台敏感配置密文。
   * @param ciphertext {@code v1:} 前缀的密文，空白值直接转换为空
   * @return 解密后的明文，输入为空时返回空
   */
  public String decrypt(String ciphertext) {
    if (ciphertext == null || ciphertext.isBlank()) return null;
    if (!ciphertext.startsWith("v1:")) throw new IllegalStateException("不支持的敏感配置版本");
    try {
      byte[] payload = Base64.getDecoder().decode(ciphertext.substring(3));
      byte[] nonce = java.util.Arrays.copyOfRange(payload, 0, 12);
      byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) { throw new IllegalStateException("平台敏感配置解密失败", e); }
  }
}
