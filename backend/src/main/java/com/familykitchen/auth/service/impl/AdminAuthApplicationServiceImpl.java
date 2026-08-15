package com.familykitchen.auth.service.impl;

import com.familykitchen.auth.model.dto.AdminLoginRequest;
import com.familykitchen.auth.model.vo.LoginResponse;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.AdminAuthApplicationService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.mapper.UserRoleMapper;
import com.familykitchen.user.model.entity.UserDO;

import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * 后台登录服务，账号来源与普通用户相同，权限来自角色关系表。
 */
@Service
public class AdminAuthApplicationServiceImpl implements AdminAuthApplicationService {
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final PasswordCodec passwordCodec;
  private final TokenService tokenService;
  private final SessionService sessionService;

  /**
   * 创建平台管理Auth实例。
   *
   * @param userMapper     用户Mapper
   * @param userRoleMapper 用户角色Mapper
   * @param passwordCodec  密码Codec
   * @param tokenService   令牌Service
   * @param sessionService 会话Service
   */
  public AdminAuthApplicationServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                         PasswordCodec passwordCodec, TokenService tokenService, SessionService sessionService) {
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.passwordCodec = passwordCodec;
    this.tokenService = tokenService;
    this.sessionService = sessionService;
  }

  /**
   * 登录平台管理Auth。
   *
   * @param request 请求参数
   * @return 登录结果后的结果
   */
  @Override
  public LoginResponse login(AdminLoginRequest request) {
    UserDO user = userMapper.findByLoginIdentifier(request.username().trim().toLowerCase());
    if (user == null || !"ACTIVE".equals(user.getStatus())
      || !passwordCodec.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }
    Set<String> roles = Set.copyOf(userRoleMapper.findActiveRoles(user.getId()));
    if (roles.isEmpty()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "该用户没有后台权限");
    }
    String roleTemplate = roles.stream().anyMatch(role -> "PLATFORM_ADMIN".equalsIgnoreCase(role))
        ? "platform_admin" : "user";
    String sessionId = sessionService.create(user.getId());
    CurrentUserContext context = new CurrentUserContext(user.getId(), null, null, null,
      roleTemplate, roles, Set.of(), sessionId);
    return new LoginResponse(tokenService.issue(context), user.getId(), null, null, null,
      roleTemplate, roles, Set.of());
  }
}
