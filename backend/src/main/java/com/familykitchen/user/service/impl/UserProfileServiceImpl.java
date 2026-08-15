package com.familykitchen.user.service.impl;

import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.service.WechatCodeExchangeClient;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.user.mapper.EmailVerificationMapper;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.model.dto.BindEmailRequest;
import com.familykitchen.user.model.dto.BindWechatRequest;
import com.familykitchen.user.model.dto.ChangePasswordRequest;
import com.familykitchen.user.model.dto.ChangeUsernameRequest;
import com.familykitchen.user.model.dto.UpdateProfileRequest;
import com.familykitchen.user.model.entity.UserDO;
import com.familykitchen.user.model.vo.UserProfileView;
import com.familykitchen.user.model.vo.UserContextView;
import com.familykitchen.auth.mapper.AuthContextMapper;
import com.familykitchen.user.service.UserProfileService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import com.familykitchen.system.service.PlatformMailService;
import com.familykitchen.system.service.SystemSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户公共账号能力服务实现，所有绑定数据来自真实数据库或第三方接口。 */
@Service
public class UserProfileServiceImpl implements UserProfileService {
  private final UserMapper userMapper;
  private final EmailVerificationMapper emailMapper;
  private final PasswordCodec passwordCodec;
  private final WechatCodeExchangeClient wechatClient;
  private final PlatformMailService mailService;
  private final SystemSettingService systemSettings;
  private final SessionService sessionService;
  private final AuthContextMapper authContextMapper;

  /**
   * 创建用户资料实例。
   *
   * @param userMapper 用户Mapper
   * @param emailMapper 邮箱Mapper
   * @param passwordCodec 密码Codec
   * @param wechatClient 微信Client
   * @param mailService mailService
   * @param sessionService 会话Service
   * @param authContextMapper auth上下文Mapper
   * @param systemSettings systemSettings
   */
  public UserProfileServiceImpl(UserMapper userMapper, EmailVerificationMapper emailMapper,
      PasswordCodec passwordCodec, WechatCodeExchangeClient wechatClient, PlatformMailService mailService,
      SessionService sessionService, AuthContextMapper authContextMapper,SystemSettingService systemSettings) {
    this.userMapper=userMapper; this.emailMapper=emailMapper; this.passwordCodec=passwordCodec;
    this.wechatClient=wechatClient; this.mailService=mailService;this.systemSettings=systemSettings;
    this.sessionService=sessionService;
    this.authContextMapper=authContextMapper;
  }

  /**
   * 处理用户资料。
   *
   * @param userId 用户标识
   * @return 处理结果
   */
  @Override public UserProfileView profile(Long userId) { return toView(requireUser(userId)); }

  /**
   * 更新资料。
   *
   * @param userId 用户标识
   * @param request 请求参数
   * @return 更新资料后的结果
   */
  @Override @Transactional
  public UserProfileView updateProfile(Long userId, UpdateProfileRequest request) {
    UserDO existing=requireUser(userId);
    if(!systemSettings.mobileBindingEnabled()&&!java.util.Objects.equals(normalizeNullable(existing.getMobile()),normalizeNullable(request.mobile())))
      throw new BusinessException(ErrorCode.FORBIDDEN,"平台未开启手机号绑定");
    userMapper.updateProfile(userId,request.nickname().trim(),request.avatarUrl(),request.mobile());
    return profile(userId);
  }

  /**
   * 修改密码。
   *
   * @param userId 用户标识
   * @param request 请求参数
   */
  @Override @Transactional
  public void changePassword(Long userId, ChangePasswordRequest request) {
    UserDO user=requireUser(userId);
    if (!passwordCodec.matches(request.currentPassword(),user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED,"原密码错误");
    }
    userMapper.updatePassword(userId,passwordCodec.encode(request.newPassword()),"BCRYPT");
    sessionService.revokeAll(userId);
  }

  /**
   * 发送邮箱编码。
   *
   * @param userId 用户标识
   * @param rawEmail raw邮箱
   * @param requestIp 请求参数Ip
   */
  @Override
  @Transactional
  public void sendEmailCode(Long userId, String rawEmail, String requestIp) {
    if(!systemSettings.emailBindingEnabled())throw new BusinessException(ErrorCode.FORBIDDEN,"平台未开启邮箱绑定");
    requireUser(userId);
    String email=normalizeEmail(rawEmail);
    UserDO occupied=userMapper.findByEmail(email);
    if (occupied!=null && !occupied.getId().equals(userId)) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_BOUND,"邮箱已绑定其他账号");
    }
    if(emailMapper.countRecent(userId,email,requestIp)>=5)
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"验证码发送过于频繁，请稍后再试");
    emailMapper.invalidatePending(userId,email);
    String code=String.format("%06d",new SecureRandom().nextInt(1_000_000));
    emailMapper.insert(userId,email,hash(code),requestIp);
    mailService.sendVerificationCode(email,code);
  }

  /**
   * 绑定邮箱。
   *
   * @param userId 用户标识
   * @param request 请求参数
   */
  @Override @Transactional
  public void bindEmail(Long userId, BindEmailRequest request) {
    if(!systemSettings.emailBindingEnabled())throw new BusinessException(ErrorCode.FORBIDDEN,"平台未开启邮箱绑定");
    String email=normalizeEmail(request.email());
    UserDO occupied=userMapper.findByEmail(email);
    if (occupied!=null && !occupied.getId().equals(userId)) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_BOUND,"邮箱已绑定其他账号");
    }
    Long recordId=emailMapper.findConsumable(userId,email,hash(request.verificationCode()));
    if (recordId==null) {
      emailMapper.incrementLatestFailure(userId,email);
      throw new BusinessException(ErrorCode.BUSINESS_INVALID,"邮箱验证码无效或已过期");
    }
    if (emailMapper.consume(recordId)==0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID,"邮箱验证码无效或已过期");
    }
    userMapper.bindEmail(userId,email);
  }

  /**
   * 绑定微信。
   *
   * @param userId 用户标识
   * @param request 请求参数
   */
  @Override @Transactional
  public void bindWechat(Long userId, BindWechatRequest request) {
    if(!systemSettings.wechatBindingEnabled())throw new BusinessException(ErrorCode.FORBIDDEN,"平台未开启微信绑定");
    requireUser(userId);
    String openId=wechatClient.exchange(request.authorizationCode());
    UserDO occupied=userMapper.findByWechatOpenId(openId);
    if (occupied!=null && !occupied.getId().equals(userId)) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"微信已绑定其他账号");
    }
    userMapper.bindWechat(userId,openId);
  }

  /**
   * 解绑微信。
   *
   * @param userId 用户标识
   */
  @Override public void unbindWechat(Long userId) {
    requireUser(userId); userMapper.unbindWechat(userId);
  }

  /**
   * 修改用户名。
   *
   * @param userId 用户标识
   * @param request 请求参数
   * @return 修改用户名后的结果
   */
  @Override @Transactional public UserProfileView changeUsername(Long userId,ChangeUsernameRequest request){
    String username=request.username().trim().toLowerCase(Locale.ROOT);
    if(userMapper.findByUsername(username)!=null)throw new BusinessException(ErrorCode.STATE_CONFLICT,"用户名已被使用");
    if(userMapper.updateUsernameOnce(userId,username)==0)throw new BusinessException(ErrorCode.BUSINESS_INVALID,"用户名修改机会已使用");
    return profile(userId);
  }

  /**
   * 处理用户资料。
   *
   * @param userId 用户标识
   * @return 处理结果
   */
  @Override public UserContextView context(Long userId){var family=authContextMapper.findActiveFamily(userId);
    var merchant=authContextMapper.findActiveMerchant(userId);return new UserContextView(userId,
      family==null?null:family.familyId(),family==null?null:family.familyRole(),merchant==null?null:merchant.merchantId(),
      merchant==null?null:merchant.merchantRole(),authContextMapper.findPlatformRoles(userId));}

  private UserDO requireUser(Long id) {
    UserDO user=userMapper.findById(id);
    if (user==null) throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在");
    return user;
  }
  private static String normalizeEmail(String value) { return value.trim().toLowerCase(Locale.ROOT); }
  private static String normalizeNullable(String value){return value==null||value.isBlank()?null:value.trim();}
  private static String hash(String value) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception e) { throw new IllegalStateException("验证码摘要计算失败",e); }
  }
  private static UserProfileView toView(UserDO user) {
    String email=user.getEmail();
    String masked=email==null?null:email.replaceAll("(^.).*(@.*$)","$1***$2");
    return new UserProfileView(user.getId(),user.getUsername(),user.getNickname(),user.getAvatarUrl(),
        user.getMobile(),masked,Boolean.TRUE.equals(user.getEmailVerified()),
        user.getWechatOpenId()!=null,user.getStatus());
  }
}
