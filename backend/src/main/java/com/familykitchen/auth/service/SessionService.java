package com.familykitchen.auth.service;
/** 用户数据库会话服务。 */
public interface SessionService {
  /** A rotating mini-program credential bound to one database session.
   * @param userId account ID
   * @param sessionId database session ID
   * @param refreshToken raw credential returned only to the client */
  record RenewableSession(Long userId, String sessionId, String refreshToken) {}

  /** Creates a mini-program session with a seven-day idle refresh window.
   * @param userId account ID
   * @return renewable session credentials */
  default RenewableSession createRenewable(Long userId) {
    throw new UnsupportedOperationException("Renewable sessions are not supported");
  }

  /** Rotates a refresh credential once, rejecting expired or replayed values.
   * @param refreshToken presented credential
   * @return replacement credentials */
  default RenewableSession rotateRefresh(String refreshToken) {
    throw new UnsupportedOperationException("Renewable sessions are not supported");
  }
  /**
   * 创建会话。
   *
   * @param userId 用户标识
   * @return 创建结果后的结果
   */
  String create(Long userId);
  /**
   * 校验并获取Active。
   *
   * @param userId 用户标识
   * @param sessionId 会话标识
   */
  void requireActive(Long userId,String sessionId);
  /**
   * 撤销会话。
   *
   * @param userId 用户标识
   * @param sessionId 会话标识
   */
  void revoke(Long userId,String sessionId);
  /**
   * 撤销All。
   *
   * @param userId 用户标识
   */
  void revokeAll(Long userId);
}
