package com.familykitchen.admin.controller;

import com.familykitchen.admin.model.dto.AdminUserRoleRequest;
import com.familykitchen.admin.model.dto.AdminUserCreateRequest;
import com.familykitchen.admin.model.dto.AdminUserStatusRequest;
import com.familykitchen.admin.model.vo.AdminUserView;
import com.familykitchen.admin.service.AdminUserService;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供平台管理用户相关的 HTTP 接口，并将请求委托给应用服务。
 */
@RestController @RequestMapping("/admin/users")
public class AdminUserController {
  private final CurrentUserProvider users; private final AdminUserService service;
  /**
   * 创建平台管理用户实例。
   *
   * @param users users
   * @param service service
   */
  public AdminUserController(CurrentUserProvider users, AdminUserService service){this.users=users;this.service=service;}
  /**
   * 处理平台管理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param keyword keyword
   * @return 列出结果后的结果
   */
  @GetMapping public ApiResponse<List<AdminUserView>> list(HttpServletRequest request,@RequestParam(required=false) String keyword){requireAdmin(request);return ApiResponse.ok(service.list(keyword));}
  /**
   * 处理平台管理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 创建结果后的结果
   */
  @PostMapping public ApiResponse<Long> create(HttpServletRequest request,@Valid @RequestBody AdminUserCreateRequest body){requireAdmin(request);return ApiResponse.ok(service.create(body));}
  /**
   * 处理平台管理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param userId 用户标识
   * @param body 请求体
   * @return 处理结果
   */
  @PutMapping("/{userId}/status") public ApiResponse<Void> status(HttpServletRequest request,@PathVariable Long userId,@Valid @RequestBody AdminUserStatusRequest body){CurrentUserContext user=requireAdmin(request);service.updateStatus(user.userId(),userId,body.status());return ApiResponse.ok();}
  /**
   * 处理平台管理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param userId 用户标识
   * @param body 请求体
   * @return 处理结果
   */
  @PutMapping("/{userId}/platform-role") public ApiResponse<Void> role(HttpServletRequest request,@PathVariable Long userId,@RequestBody AdminUserRoleRequest body){CurrentUserContext user=requireAdmin(request);service.updatePlatformRole(user.userId(),userId,body.platformAdmin());return ApiResponse.ok();}
  /**
   * 处理平台管理用户相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param userId 用户标识
   * @return 删除结果后的结果
   */
  @DeleteMapping("/{userId}") public ApiResponse<Void> delete(HttpServletRequest request,@PathVariable Long userId){CurrentUserContext user=requireAdmin(request);service.delete(user.userId(),userId);return ApiResponse.ok();}
  private CurrentUserContext requireAdmin(HttpServletRequest request){CurrentUserContext user=users.require(request);if(!user.hasPlatformBackendAccess())throw new BusinessException(ErrorCode.FORBIDDEN,"无平台管理员权限");return user;}
}
