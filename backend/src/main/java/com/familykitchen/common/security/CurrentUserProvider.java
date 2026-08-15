package com.familykitchen.common.security;

import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户提供器。
 *
 * <p>Token 用于定位用户与登录会话；家庭、商户和平台权限均从数据库实时装载。</p>
 */
@Component
public class CurrentUserProvider {
  private final TokenService tokenService;
  private final IdentityContextMapper identityContextMapper;
  private final SessionService sessionService;

  /**
   * 创建当前用户上下文提供器。
   * @param tokenService 令牌解析服务
   * @param identityContextMapper 实时身份关系查询接口
   * @param sessionService 登录会话校验服务
   */
  public CurrentUserProvider(TokenService tokenService, IdentityContextMapper identityContextMapper,
      SessionService sessionService) {
    this.tokenService = tokenService;
    this.identityContextMapper = identityContextMapper;
    this.sessionService = sessionService;
  }

  /**
   * 校验请求身份与会话，并以数据库中的最新关系组装权限上下文。
   * <p>令牌只承担账号和会话定位，家庭、商户及后台权限每次实时查询，避免关系变更后旧令牌继续越权。</p>
   * @param request 当前 HTTP 请求
   * @return 已通过身份与会话校验的用户上下文
   */
  public CurrentUserContext require(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少有效的 Bearer 令牌");
    }
    CurrentUserContext tokenUser = tokenService.parse(authorization.substring(7).trim());
    sessionService.requireActive(tokenUser.userId(), tokenUser.sessionId());
    IdentityContextRow identity = identityContextMapper.findIdentity(tokenUser.userId());
    if (identity == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不可用或登录已失效");
    }
    Set<String> platformRoles = new LinkedHashSet<>(identityContextMapper.findPlatformRoles(identity.getUserId()));
    Set<String> merchantScopes = identity.getMerchantId() == null ? Set.of()
        : new LinkedHashSet<>(identityContextMapper.findMerchantScopes(identity.getUserId(), identity.getMerchantId()));
    String role = identity.getFamilyRole() == null ? "user" : identity.getFamilyRole().toLowerCase();
    return new CurrentUserContext(identity.getUserId(), identity.getMerchantId(), identity.getFamilyId(),
        identity.getUserId(), role, Set.copyOf(platformRoles), Set.copyOf(merchantScopes), tokenUser.sessionId());
  }
}
