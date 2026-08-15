package com.familykitchen.auth.service.impl;

import com.familykitchen.auth.mapper.UserSessionMapper;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * 用户数据库会话服务实现。
 */
@Service
public class SessionServiceImpl implements SessionService {
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
