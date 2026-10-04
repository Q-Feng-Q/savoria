package com.familykitchen.auth.service.impl;

import com.familykitchen.auth.mapper.UserSessionMapper;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * 用户数据库会话服务实现。
 */
@Service
public class SessionServiceImpl implements SessionService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final UserSessionMapper mapper;

  /**
   * 创建会话实例。
   *
   * @param mapper mapper
   */
  public SessionServiceImpl(UserSessionMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * 创建会话。
   *
   * @param userId 用户标识
   * @return 创建结果后的结果
   */
  public String create(Long userId) {
    String id = UUID.randomUUID().toString();
    mapper.insert(id, userId, hash(id));
    return id;
  }

  /** Creates a seven-day renewable mini-program session.
   * @param userId account ID
   * @return initial credentials */
  @Override
  public RenewableSession createRenewable(Long userId) {
    String id = UUID.randomUUID().toString();
    String refreshToken = newRefreshToken(id);
    mapper.insertRenewable(id, userId, hash(refreshToken));
    return new RenewableSession(userId, id, refreshToken);
  }

  /** Rotates the refresh credential using one conditional database update.
   * @param refreshToken presented credential
   * @return replacement credentials */
  @Override
  public RenewableSession rotateRefresh(String refreshToken) {
    if (refreshToken == null || !refreshToken.matches(
        "[0-9a-fA-F-]{36}\\.[A-Za-z0-9_-]{43}")) throw invalidRefresh();
    String id = refreshToken.substring(0, 36);
    Long userId = mapper.findRenewableUser(id);
    if (userId == null) throw invalidRefresh();
    String replacement = newRefreshToken(id);
    if (mapper.rotateRefresh(id, userId, hash(refreshToken), hash(replacement)) != 1)
      throw invalidRefresh();
    return new RenewableSession(userId, id, replacement);
  }

  private static String newRefreshToken(String id) {
    byte[] secret = new byte[32];
    RANDOM.nextBytes(secret);
    return id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
  }

  private static BusinessException invalidRefresh() {
    return new BusinessException(ErrorCode.UNAUTHORIZED, "续期凭证已失效，请重新登录");
  }

  /**
   * 校验并获取Active。
   *
   * @param userId 用户标识
   * @param id     标识
   */
  public void requireActive(Long userId, String id) {
    if (id == null || mapper.countActive(id, userId) == 0)
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话已失效");
  }

  /**
   * 撤销会话。
   *
   * @param userId 用户标识
   * @param id     标识
   */
  public void revoke(Long userId, String id) {
    if (id != null) mapper.revoke(id, userId);
  }

  /**
   * 撤销All。
   *
   * @param userId 用户标识
   */
  public void revokeAll(Long userId) {
    mapper.revokeAll(userId);
  }

  private static String hash(String v) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(v.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
