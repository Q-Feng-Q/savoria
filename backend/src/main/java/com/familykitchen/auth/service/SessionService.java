package com.familykitchen.auth.service;
/** 用户数据库会话服务。 */
public interface SessionService {
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
