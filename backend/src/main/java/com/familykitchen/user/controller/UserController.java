package com.familykitchen.user.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.user.model.dto.BindEmailRequest;
import com.familykitchen.user.model.dto.BindWechatRequest;
import com.familykitchen.user.model.dto.ChangePasswordRequest;
import com.familykitchen.user.model.dto.ChangeUsernameRequest;
import com.familykitchen.user.model.dto.AccountCancellationRequest;
import com.familykitchen.user.model.dto.EmailCodeRequest;
import com.familykitchen.user.model.dto.UpdateProfileRequest;
import com.familykitchen.user.model.vo.UserProfileView;
import com.familykitchen.user.model.vo.UserContextView;
import com.familykitchen.user.service.AccountLifecycleService;
import com.familykitchen.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 用户公共账号接口，不依赖家庭关系。 */
@RestController
@RequestMapping("/users/me")
@Tag(name = "用户-个人账号", description = "个人资料、密码和账号绑定接口")
public class UserController {
  private final CurrentUserProvider currentUserProvider;
  private final UserProfileService userProfileService;
  private final AccountLifecycleService accountLifecycleService;

  /**
   * 创建用户实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param userProfileService 用户资料Service
   * @param accountLifecycleService accountLifecycleService
   */
  public UserController(CurrentUserProvider currentUserProvider, UserProfileService userProfileService,
                        AccountLifecycleService accountLifecycleService) {
    this.currentUserProvider = currentUserProvider;
    this.userProfileService = userProfileService;
    this.accountLifecycleService = accountLifecycleService;
  }

  /**
   * 处理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理结果
   */
  @GetMapping("/context")
  @Operation(summary = "查询当前账号的家庭、商户和平台权限上下文")
  public ApiResponse<UserContextView> context(HttpServletRequest request) {
    var user = currentUserProvider.require(request);
    String familyRole = user.familyId() == null ? null : user.roleTemplate();
    boolean merchantAccess = user.merchantId() != null && user.hasMerchantBackendAccess();
    boolean platformAccess = user.hasPlatformBackendAccess();
    String merchantRole = merchantAccess ? user.merchantAdminScopes().stream()
        .filter(scope -> "MERCHANT_ADMIN".equalsIgnoreCase(scope) || "merchant".equalsIgnoreCase(scope))
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .findFirst()
        .orElse("MERCHANT_ADMIN") : null;

    List<String> availableModes = new ArrayList<>();
    if (user.familyId() != null || (!merchantAccess && !platformAccess)) {
      availableModes.add("family");
    }
    if (merchantAccess) {
      availableModes.add("merchant");
    }

    List<String> permissionCodes = new ArrayList<>();
    if (user.familyId() != null) {
      permissionCodes.add("FAMILY_MEMBER");
    }
    if (user.hasFamilyAdminAccess()) {
      permissionCodes.add("FAMILY_ADMIN");
    }
    if (merchantAccess) {
      permissionCodes.add("MERCHANT_ADMIN");
    }
    if (platformAccess) {
      permissionCodes.add("PLATFORM_ADMIN");
    }

    List<String> platformRoles = user.backendRoles().stream()
        .sorted(Comparator.naturalOrder())
        .toList();
    return ApiResponse.ok(new UserContextView(
        user.userId(),
        user.familyId(),
        familyRole,
        user.merchantId(),
        merchantRole,
        platformRoles,
        List.copyOf(availableModes),
        List.copyOf(permissionCodes)));
  }

  /**
   * 处理用户名相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 修改用户名后的结果
   */
  @PutMapping("/username")
  @Operation(summary = "修改用户名", description = "每个账号仅允许修改一次用户名。")
  public ApiResponse<UserProfileView> changeUsername(HttpServletRequest request,
      @Valid @RequestBody ChangeUsernameRequest body) {
    return ApiResponse.ok(userProfileService.changeUsername(currentUserProvider.require(request).userId(), body));
  }

  /**
   * 处理Cancellation相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 处理Cancellation后的结果
   */
  @PostMapping("/cancellation")
  @Operation(summary = "申请注销账号", description = "账号进入七天冷静期，并立即注销全部登录会话。")
  public ApiResponse<Void> requestCancellation(HttpServletRequest request,
      @Valid @RequestBody AccountCancellationRequest body) {
    accountLifecycleService.request(currentUserProvider.require(request).userId(), body.password());
    return ApiResponse.ok();
  }

  /**
   * 处理Cancellation相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 取消Cancellation后的结果
   */
  @DeleteMapping("/cancellation")
  @Operation(summary = "撤销账号注销申请")
  public ApiResponse<Void> cancelCancellation(HttpServletRequest request) {
    accountLifecycleService.cancel(currentUserProvider.require(request).userId());
    return ApiResponse.ok();
  }

  /**
   * 处理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理结果
   */
  @GetMapping
  @Operation(summary = "查询个人资料")
  public ApiResponse<UserProfileView> profile(HttpServletRequest request) {
    return ApiResponse.ok(userProfileService.profile(currentUserProvider.require(request).userId()));
  }

  /**
   * 处理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 更新结果后的结果
   */
  @PutMapping
  @Operation(summary = "修改个人资料")
  public ApiResponse<UserProfileView> update(HttpServletRequest request,
                                              @Valid @RequestBody UpdateProfileRequest body) {
    return ApiResponse.ok(userProfileService.updateProfile(currentUserProvider.require(request).userId(), body));
  }

  /**
   * 处理密码相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 修改密码后的结果
   */
  @PutMapping("/password")
  @Operation(summary = "修改密码")
  public ApiResponse<Void> changePassword(HttpServletRequest request,
                                           @Valid @RequestBody ChangePasswordRequest body) {
    userProfileService.changePassword(currentUserProvider.require(request).userId(), body);
    return ApiResponse.ok();
  }

  /**
   * 处理邮箱相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 绑定邮箱后的结果
   */
  @PutMapping("/email")
  @Operation(summary = "绑定或更换邮箱")
  public ApiResponse<Void> bindEmail(HttpServletRequest request, @Valid @RequestBody BindEmailRequest body) {
    userProfileService.bindEmail(currentUserProvider.require(request).userId(), body);
    return ApiResponse.ok();
  }

  /**
   * 处理邮箱编码相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 发送邮箱编码后的结果
   */
  @PostMapping("/email/code")
  @Operation(summary = "发送邮箱绑定验证码")
  public ApiResponse<Void> sendEmailCode(HttpServletRequest request,
                                          @Valid @RequestBody EmailCodeRequest body) {
    userProfileService.sendEmailCode(currentUserProvider.require(request).userId(), body.email(), request.getRemoteAddr());
    return ApiResponse.ok();
  }

  /**
   * 处理微信相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 绑定微信后的结果
   */
  @PostMapping("/wechat")
  @Operation(summary = "绑定微信")
  public ApiResponse<Void> bindWechat(HttpServletRequest request, @Valid @RequestBody BindWechatRequest body) {
    userProfileService.bindWechat(currentUserProvider.require(request).userId(), body);
    return ApiResponse.ok();
  }

  /**
   * 处理微信相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 解绑微信后的结果
   */
  @DeleteMapping("/wechat")
  @Operation(summary = "解绑微信")
  public ApiResponse<Void> unbindWechat(HttpServletRequest request) {
    userProfileService.unbindWechat(currentUserProvider.require(request).userId());
    return ApiResponse.ok();
  }
}
