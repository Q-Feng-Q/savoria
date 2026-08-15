package com.familykitchen.auth.service;
import com.familykitchen.auth.model.dto.PasswordResetRequest;
/** 邮箱密码找回服务。 */
public interface PasswordRecoveryService {
  /**
   * 发送编码。
   *
   * @param email 邮箱
   * @param requestIp 请求参数Ip
   */
  void sendCode(String email, String requestIp);
  /**
   * 处理密码Recovery。
   *
   * @param request 请求参数
   */
  void reset(PasswordResetRequest request);
}
