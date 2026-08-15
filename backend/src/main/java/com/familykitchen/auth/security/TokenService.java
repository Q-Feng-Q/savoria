package com.familykitchen.auth.security;

import com.familykitchen.auth.config.AuthTokenProperties;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户访问令牌服务。
 *
 * <p>令牌只保存用户 ID 和有效期，家庭及权限信息由数据库实时解析。</p>
 */
@Component
public class TokenService {
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private final ObjectMapper objectMapper;
  private final String secret;
  private final long expiresInSeconds;

  /**
   * 创建令牌实例。
   *
   * @param objectMapper objectMapper
   * @param properties properties
   */
  @Autowired
  public TokenService(ObjectMapper objectMapper, AuthTokenProperties properties) {
    this(objectMapper, properties.secret(), properties.expiresInSeconds());
  }

  /**
   * 创建令牌实例。
   *
   * @param objectMapper objectMapper
   * @param secret secret
   * @param expiresInSeconds expiresInSeconds
   */
  public TokenService(ObjectMapper objectMapper, String secret, long expiresInSeconds) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT签名密钥未配置，请设置环境变量 FAMILY_KITCHEN_JWT_SECRET");
    }
    this.objectMapper = objectMapper;
    this.secret = secret;
    this.expiresInSeconds = expiresInSeconds;
  }

  /** 为用户签发不携带业务权限的令牌。
   * @param context 上下文
   * @return 处理结果
   */
  public String issue(CurrentUserContext context) {
    return issue(context.userId(), context.sessionId());
  }

  /** 为指定数据库会话签发访问令牌。
   * @param userId 用户标识
   * @param sessionId 会话标识
   * @return 处理结果
   */
  public String issue(Long userId, String sessionId) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("userId", userId);
      payload.put("sessionId", sessionId);
      payload.put("exp", Instant.now().getEpochSecond() + expiresInSeconds);
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
          objectMapper.writeValueAsBytes(payload));
      return encoded + "." + sign(encoded);
    } catch (Exception exception) {
      throw new IllegalStateException("访问令牌签发失败", exception);
    }
  }

  /** 校验令牌并返回最小用户上下文。
   * @param token 令牌
   * @return 处理结果
   */
  public CurrentUserContext parse(String token) {
    try {
      String[] parts = token == null ? new String[0] : token.split("\\.");
      if (parts.length != 2 || !constantEquals(sign(parts[0]), parts[1])) {
        throw unauthorized();
      }
      Map<String, Object> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), MAP_TYPE);
      long expiresAt = number(payload.get("exp"));
      if (Instant.now().getEpochSecond() > expiresAt) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "访问令牌已过期");
      }
      Long userId = number(payload.get("userId"));
      String sessionId = String.valueOf(payload.get("sessionId"));
      return new CurrentUserContext(userId, null, null, null, "user", java.util.Set.of(), java.util.Set.of(), sessionId);
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw unauthorized();
    }
  }

  private String sign(String payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(
        mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
  }

  private static long number(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    throw unauthorized();
  }

  private static boolean constantEquals(String left, String right) {
    if (left.length() != right.length()) return false;
    int difference = 0;
    for (int i = 0; i < left.length(); i++) difference |= left.charAt(i) ^ right.charAt(i);
    return difference == 0;
  }

  private static BusinessException unauthorized() {
    return new BusinessException(ErrorCode.UNAUTHORIZED, "访问令牌无效");
  }
}
