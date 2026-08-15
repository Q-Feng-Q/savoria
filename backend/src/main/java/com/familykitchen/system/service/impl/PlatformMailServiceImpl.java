package com.familykitchen.system.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.model.entity.SystemSettingDO;
import com.familykitchen.system.security.PlatformSecretCipher;
import com.familykitchen.system.service.PlatformMailService;
import java.util.Properties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/** 每次发送时读取平台 SMTP 配置并即时构造邮件发送器的实现。 */
@Service
public class PlatformMailServiceImpl implements PlatformMailService {
  private final SystemSettingMapper settings;
  private final PlatformSecretCipher cipher;
  /**
   * 创建平台邮件服务。
   * @param settings 系统配置查询接口
   * @param cipher 平台敏感配置解密器
   */
  public PlatformMailServiceImpl(SystemSettingMapper settings, PlatformSecretCipher cipher) { this.settings=settings; this.cipher=cipher; }
  /**
   * 发送带有效期和安全提示的邮箱验证码。
   * @param recipient 收件邮箱
   * @param code 验证码明文
   */
  @Override public void sendVerificationCode(String recipient,String code) {
    send(recipient,"食光知味邮箱验证码","您的验证码是："+code+"，10分钟内有效。请勿转发给他人。");
  }
  /**
   * 使用当前平台配置发送 SMTP 测试邮件。
   * @param recipient 接收 SMTP 配置测试邮件的邮箱
   */
  @Override public void sendTestMail(String recipient) {
    send(recipient,"食光知味邮件服务测试","邮件发送成功，平台 SMTP 配置已生效。");
  }
  private void send(String recipient,String subject,String body) {
    SystemSettingDO s=settings.selectCurrent();
    // 发送能力受公开业务开关和完整凭据双重约束，防止关闭绑定后仍从其他入口发信。
    if(s==null||!Boolean.TRUE.equals(s.getEmailBindingEnabled())) throw new BusinessException(ErrorCode.FORBIDDEN,"平台未开启邮箱绑定");
    if(blank(s.getSmtpHost())||s.getSmtpPort()==null||blank(s.getSmtpUsername())||blank(s.getSmtpPasswordCiphertext())||blank(s.getSmtpFrom()))
      throw new BusinessException(ErrorCode.BUSINESS_INVALID,"平台邮件服务尚未配置完整");
    JavaMailSenderImpl sender=new JavaMailSenderImpl(); sender.setHost(s.getSmtpHost()); sender.setPort(s.getSmtpPort());
    sender.setUsername(s.getSmtpUsername()); sender.setPassword(cipher.decrypt(s.getSmtpPasswordCiphertext()));
    Properties p=sender.getJavaMailProperties(); p.put("mail.smtp.auth","true"); p.put("mail.smtp.starttls.enable",String.valueOf(Boolean.TRUE.equals(s.getSmtpTlsEnabled())));
    SimpleMailMessage message=new SimpleMailMessage(); message.setFrom(s.getSmtpFrom()); message.setTo(recipient); message.setSubject(subject); message.setText(body); sender.send(message);
  }
  private static boolean blank(String v){return v==null||v.isBlank();}
}
