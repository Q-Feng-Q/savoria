package com.familykitchen.auth.controller;

import com.familykitchen.auth.model.dto.AdminLoginRequest;
import com.familykitchen.auth.model.dto.RegisterRequest;
import com.familykitchen.auth.model.dto.PasswordResetCodeRequest;
import com.familykitchen.auth.model.dto.PasswordResetRequest;
import com.familykitchen.auth.model.dto.UserLoginRequest;
import com.familykitchen.auth.model.dto.WechatLoginRequest;
import com.familykitchen.auth.model.vo.LoginResponse;
import com.familykitchen.auth.service.AdminAuthApplicationService;
import com.familykitchen.auth.service.MemberAuthService;
import com.familykitchen.auth.service.PasswordRecoveryService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 统一用户认证接口。 */
@RestController
@RequestMapping("/auth")
@Tag(name = "公共-认证", description = "统一用户注册、登录和微信授权")
public class AuthController {
  private final AdminAuthApplicationService adminAuthService;
  private final MemberAuthService memberAuthService;
  private final PasswordRecoveryService passwordRecoveryService;
  private final CurrentUserProvider currentUserProvider;
  private final SessionService sessionService;

  /**
   * 创建Auth实例。
   *
   * @param adminAuthService 平台管理AuthService
   * @param memberAuthService 成员AuthService
   * @param passwordRecoveryService 密码RecoveryService
   * @param currentUserProvider 当前用户Provider
   * @param sessionService 会话Service
   */
  public AuthController(AdminAuthApplicationService adminAuthService, MemberAuthService memberAuthService,
                        PasswordRecoveryService passwordRecoveryService,CurrentUserProvider currentUserProvider,
                        SessionService sessionService) {
    this.adminAuthService = adminAuthService;
    this.memberAuthService = memberAuthService;
    this.passwordRecoveryService = passwordRecoveryService;
    this.currentUserProvider=currentUserProvider;this.sessionService=sessionService;
  }

  /**
   * 处理Auth相关的 HTTP 请求。
   *
   * @param body 请求体
   * @return 注册结果后的结果
   */
  @PostMapping("/register")
  @Operation(summary = "注册独立用户", description = "注册成功后可以不属于任何家庭。")
  public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest body) {
    return ApiResponse.ok(memberAuthService.register(body));
  }

  /**
   * 处理Auth相关的 HTTP 请求。
   *
   * @param body 请求体
   * @return 登录结果后的结果
   */
  @PostMapping("/login")
  @Operation(summary = "用户登录", description = "支持用户名或已验证邮箱加密码登录。")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest body) {
    return ApiResponse.ok(memberAuthService.login(body));
  }

  /**
   * 处理Login相关的 HTTP 请求。
   *
   * @param body 请求体
   * @return 处理Login后的结果
   */
  @PostMapping("/admin/login")
  @Operation(summary = "后台登录", description = "账号仍来自统一用户表，并额外校验后台角色关系。")
  public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest body) {
    return ApiResponse.ok(adminAuthService.login(body));
  }

  /**
   * 处理Login相关的 HTTP 请求。
   *
   * @param body 请求体
   * @return 处理Login后的结果
   */
  @PostMapping("/wechat/login")
  @Operation(summary = "微信登录", description = "使用微信临时登录码换取用户登录态。")
  public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest body) {
    return ApiResponse.ok(memberAuthService.wechatLogin(body));
  }

  /**
   * 处理Reset编码相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 发送Reset编码后的结果
   */
  @PostMapping("/password/reset-code")
  @Operation(summary = "发送密码找回验证码", description = "无论邮箱是否存在均返回统一受理结果。")
  public ApiResponse<Void> sendResetCode(HttpServletRequest request,
                                          @Valid @RequestBody PasswordResetCodeRequest body) {
    passwordRecoveryService.sendCode(body.email(), request.getRemoteAddr());
    return ApiResponse.ok();
  }

  /**
   * 处理密码相关的 HTTP 请求。
   *
   * @param body 请求体
   * @return 处理密码后的结果
   */
  @PostMapping("/password/reset")
  @Operation(summary = "通过邮箱验证码重置密码")
  public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest body) {
    passwordRecoveryService.reset(body);
    return ApiResponse.ok();
  }

  /**
   * 处理Auth相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 退出结果后的结果
   */
  @PostMapping("/logout")
  @Operation(summary="退出当前设备")
  public ApiResponse<Void> logout(HttpServletRequest request){var user=currentUserProvider.require(request);
    sessionService.revoke(user.userId(),user.sessionId());return ApiResponse.ok();}

  /**
   * 处理All相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 退出All后的结果
   */
  @PostMapping("/logout-all")
  @Operation(summary="退出全部设备")
  public ApiResponse<Void> logoutAll(HttpServletRequest request){var user=currentUserProvider.require(request);
    sessionService.revokeAll(user.userId());return ApiResponse.ok();}
}
