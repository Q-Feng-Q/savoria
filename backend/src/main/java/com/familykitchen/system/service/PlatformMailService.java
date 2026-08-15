package com.familykitchen.system.service;

/** 基于平台当前 SMTP 配置发送验证类与运维测试邮件。 */
public interface PlatformMailService {
  /**
   * 发送邮箱验证码。
   * @param recipient 收件邮箱
   * @param code 验证码明文
   */
  void sendVerificationCode(String recipient, String code);
  /**
   * 发送用于验证平台 SMTP 配置的测试邮件。
   * @param recipient 接收平台配置测试邮件的邮箱
   */
  void sendTestMail(String recipient);
}
