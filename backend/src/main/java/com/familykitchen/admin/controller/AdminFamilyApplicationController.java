package com.familykitchen.admin.controller;

import com.familykitchen.admin.model.dto.ApproveRequest;
import com.familykitchen.admin.service.AdminFamilyApplicationService;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.family.model.entity.FamilyApplicationDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台家庭申请管理控制器。
 */
@RestController
@RequestMapping("/admin/family-applications")
@Tag(name = "后台-家庭申请管理", description = "平台管理员审批家庭创建申请")
public class AdminFamilyApplicationController {

  private final CurrentUserProvider currentUserProvider;
  private final AdminFamilyApplicationService adminFamilyApplicationService;

  /**
   * 创建平台管理家庭申请实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param adminFamilyApplicationService 平台管理家庭申请Service
   */
  public AdminFamilyApplicationController(
    CurrentUserProvider currentUserProvider,
    AdminFamilyApplicationService adminFamilyApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.adminFamilyApplicationService = adminFamilyApplicationService;
  }

  /**
   * 处理平台管理家庭申请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param status 状态
   * @return 列出结果后的结果
   */
  @GetMapping
  @Operation(summary = "查询申请列表", description = "查询家庭创建申请列表。")
  public ApiResponse<List<FamilyApplicationDO>> list(
    HttpServletRequest request,
    @RequestParam(required = false) String status
  ) {
    CurrentUserContext user = requirePlatformAdmin(request);
    return ApiResponse.ok(adminFamilyApplicationService.listApplications(status));
  }

  /**
   * 处理平台管理家庭申请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @param body 请求体
   * @return 批准结果后的结果
   */
  @PostMapping("/{id}/approve")
  @Operation(summary = "审批通过", description = "审批通过家庭创建申请。")
  public ApiResponse<Void> approve(
    HttpServletRequest request,
    @PathVariable Long id,
    @RequestBody(required = false) ApproveRequest body
  ) {
    CurrentUserContext user = requirePlatformAdmin(request);
    adminFamilyApplicationService.approve(user, id, body != null ? body.remark() : null);
    return ApiResponse.ok();
  }

  /**
   * 处理平台管理家庭申请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @param body 请求体
   * @return 拒绝结果后的结果
   */
  @PostMapping("/{id}/reject")
  @Operation(summary = "审批拒绝", description = "审批拒绝家庭创建申请。")
  public ApiResponse<Void> reject(
    HttpServletRequest request,
    @PathVariable Long id,
    @RequestBody(required = false) ApproveRequest body
  ) {
    CurrentUserContext user = requirePlatformAdmin(request);
    adminFamilyApplicationService.reject(user, id, body != null ? body.remark() : null);
    return ApiResponse.ok();
  }

  private CurrentUserContext requirePlatformAdmin(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasPlatformBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理员权限");
    }
    return user;
  }
}
