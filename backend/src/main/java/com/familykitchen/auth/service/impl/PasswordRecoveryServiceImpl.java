package com.familykitchen.auth.service.impl;

import com.familykitchen.auth.mapper.PasswordResetMapper;
import com.familykitchen.auth.model.dto.PasswordResetRequest;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.service.PasswordRecoveryService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.model.entity.UserDO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用真实邮件服务完成密码找回。
 */
@Service
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {
  private final UserMapper userMapper;
  private final PasswordResetMapper resetMapper;
  private final PasswordCodec passwordCodec;
  private final JavaMailSender mailSender;
  private final String from;
  private final SessionService sessionService;

  /**
   * 创建密码Recovery实例。
   *
   * @param userMapper     用户Mapper
   * @param resetMapper    resetMapper
   * @param passwordCodec  密码Codec
   * @param mailSender     mailSender
   * @param from           from
   * @param sessionService 会话Service
   */
  public PasswordRecoveryServiceImpl(UserMapper userMapper, PasswordResetMapper resetMapper,
                                     PasswordCodec passwordCodec, JavaMailSender mailSender,
                                     @Value("#{environment.getProperty('spring.mail.username','')}") String from, SessionService sessionService) {
    this.userMapper = userMapper;
    this.resetMapper = resetMapper;
    this.passwordCodec = passwordCodec;
    this.mailSender = mailSender;
    this.from = from;
    this.sessionService = sessionService;
  }

  /**
   * 发送编码。
   *
   * @param rawEmail  raw邮箱
   * @param requestIp 请求参数Ip
   */
  @Override
  public void sendCode(String rawEmail, String requestIp) {
    String email = rawEmail.trim().toLowerCase(Locale.ROOT);
    UserDO user = userMapper.findByEmail(email);
    if (user == null || !Boolean.TRUE.equals(user.getEmailVerified())) return;
    if (resetMapper.countRecent(email, requestIp) >= 5) return;
    resetMapper.invalidatePending(email);
    String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
    resetMapper.insert(user.getId(), email, hash(code), requestIp);
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject("家庭厨房密码找回验证码");
    message.setText("您的验证码是：" + code + "，10分钟内有效。请勿转发给他人。");
    mailSender.send(message);
  }

  /**
   * 处理密码Recovery。
   *
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void reset(PasswordResetRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    Long recordId = resetMapper.findConsumable(email, hash(request.code()));
    UserDO user = userMapper.findByEmail(email);
    if (recordId == null) {
      resetMapper.incrementLatestFailure(email);
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "验证码无效或已过期");
    }
    if (user == null || resetMapper.consume(recordId) == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "验证码无效或已过期");
    }
    userMapper.updatePassword(user.getId(), passwordCodec.encode(request.newPassword()), "BCRYPT");
    sessionService.revokeAll(user.getId());
  }

  private static String hash(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("验证码摘要计算失败", exception);
    }
  }
}
