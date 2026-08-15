package com.familykitchen.auth.service.impl;

import com.familykitchen.auth.model.dto.RegisterRequest;
import com.familykitchen.auth.model.dto.UserLoginRequest;
import com.familykitchen.auth.model.dto.WechatLoginRequest;
import com.familykitchen.auth.mapper.AuthContextMapper;
import com.familykitchen.auth.model.vo.LoginResponse;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.MemberAuthService;
import com.familykitchen.auth.service.WechatCodeExchangeClient;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.model.entity.UserDO;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.entity.WalletAccountDO;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统一用户注册和密码登录服务。
 */
@Service
public class MemberAuthServiceImpl implements MemberAuthService {
  private final UserMapper userMapper;
  private final PasswordCodec passwordCodec;
  private final TokenService tokenService;
  private final WechatCodeExchangeClient wechatClient;
  private final SessionService sessionService;
  private final AuthContextMapper authContextMapper;
  private final WalletPersistenceMapper walletMapper;

  /**
   * 创建成员Auth实例。
   *
   * @param userMapper        用户Mapper
   * @param passwordCodec     密码Codec
   * @param tokenService      令牌Service
   * @param wechatClient      微信Client
   * @param sessionService    会话Service
   * @param authContextMapper auth上下文Mapper
   * @param walletMapper      钱包Mapper
   */
  public MemberAuthServiceImpl(UserMapper userMapper, PasswordCodec passwordCodec, TokenService tokenService,
                               WechatCodeExchangeClient wechatClient, SessionService sessionService,
                               AuthContextMapper authContextMapper, WalletPersistenceMapper walletMapper) {
    this.userMapper = userMapper;
    this.passwordCodec = passwordCodec;
    this.tokenService = tokenService;
    this.wechatClient = wechatClient;
    this.sessionService = sessionService;
    this.authContextMapper = authContextMapper;
    this.walletMapper = walletMapper;
  }

  /**
   * 注册独立用户；注册成功时用户可以尚未加入任何家庭。
   *
   * @param request 请求参数
   * @return 注册结果后的结果
   */
  @Override
  @Transactional
  public LoginResponse register(RegisterRequest request) {
    String username = normalizeUsername(request.username());
    if (userMapper.findByUsername(username) != null) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "用户名已被使用");
    }
    UserDO user = new UserDO();
    user.setUsername(username);
    user.setPasswordHash(passwordCodec.encode(request.password()));
    user.setPasswordAlgorithm("BCRYPT");
    user.setCredentialStatus("ACTIVE");
    user.setNickname(request.name().trim());
    user.setMobile(normalizeNullable(request.mobile()));
    user.setStatus("ACTIVE");
    userMapper.insert(user);
    WalletAccountDO wallet = new WalletAccountDO();
    wallet.setMemberId(user.getId());
    wallet.setBalanceAmount(BigDecimal.ZERO);
    wallet.setFrozenAmount(BigDecimal.ZERO);
    walletMapper.insertWalletAccount(wallet);
    return loginResponse(user.getId());
  }

  /**
   * 使用用户名或已验证邮箱登录。
   *
   * @param request 请求参数
   * @return 登录结果后的结果
   */
  @Override
  @Transactional
  public LoginResponse login(UserLoginRequest request) {
    String identifier = request.username().trim().toLowerCase(Locale.ROOT);
    UserDO user = userMapper.findByLoginIdentifier(identifier);
    if (user == null || !"ACTIVE".equals(user.getStatus())
      || !"ACTIVE".equals(user.getCredentialStatus())
      || !passwordCodec.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    if (passwordCodec.requiresUpgrade(user.getPasswordHash())) {
      userMapper.updatePassword(user.getId(), passwordCodec.encode(request.password()), "BCRYPT");
    }
    userMapper.updateLastLogin(user.getId());
    return loginResponse(user.getId());
  }

  /**
   * 处理Login。
   *
   * @param request 请求参数
   * @return 处理Login后的结果
   */
  @Override
  public LoginResponse wechatLogin(WechatLoginRequest request) {
    UserDO user = userMapper.findByWechatOpenId(wechatClient.exchange(request.code()));
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "该微信尚未绑定用户账号");
    }
    if (!"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "账号不可用");
    }
    userMapper.updateLastLogin(user.getId());
    return loginResponse(user.getId());
  }

  private LoginResponse loginResponse(Long userId) {
    String sessionId = sessionService.create(userId);
    AuthContextMapper.FamilyContext family = authContextMapper.findActiveFamily(userId);
    AuthContextMapper.MerchantContext merchant = authContextMapper.findActiveMerchant(userId);
    Set<String> roles = Set.copyOf(authContextMapper.findPlatformRoles(userId));
    Long merchantId = merchant != null ? merchant.merchantId() : (family == null ? null : family.merchantId());
    Long familyId = family == null ? null : family.familyId();
    String roleTemplate;
    if (merchant != null) roleTemplate = "merchant_admin";
    else if (family != null)
      roleTemplate = "OWNER".equals(family.familyRole()) || "ADMIN".equals(family.familyRole()) ? "admin" : "member";
    else if (roles.contains("platform_admin")) roleTemplate = "platform_admin";
    else roleTemplate = "user";
    Set<String> backendRoles = merchant == null ? roles : mergeRoles(roles, merchant.merchantRole());
    Set<String> scopes = merchant == null ? Set.of() : Set.of("merchant");
    CurrentUserContext context = new CurrentUserContext(
      userId, merchantId, familyId, userId, roleTemplate, backendRoles, scopes, sessionId
    );
    return new LoginResponse(tokenService.issue(context), userId, merchantId, familyId, userId,
      roleTemplate, backendRoles, scopes);
  }

  private static Set<String> mergeRoles(Set<String> roles, String merchantRole) {
    java.util.HashSet<String> merged = new java.util.HashSet<>(roles);
    if (merchantRole != null && !merchantRole.isBlank()) merged.add(merchantRole.toLowerCase(Locale.ROOT));
    merged.add("merchant_admin");
    return Set.copyOf(merged);
  }

  private static String normalizeUsername(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
