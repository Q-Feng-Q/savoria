package com.familykitchen.user.service;
/** 账号注销冷静期服务。 */
public interface AccountLifecycleService {
  /**
   * 处理AccountLifecycle。
   *
   * @param userId 用户标识
   * @param password 密码
   */
  void request(Long userId,String password);
  /**
   * 取消AccountLifecycle。
   *
   * @param userId 用户标识
   */
  void cancel(Long userId);
  /**
   * 处理Due。
   */
  void processDue();
}
