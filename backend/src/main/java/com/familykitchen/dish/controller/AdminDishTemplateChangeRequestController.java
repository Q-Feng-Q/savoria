package com.familykitchen.dish.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.AdminDishTemplateChangeQuery;
import com.familykitchen.dish.model.dto.DishTemplateApproveRequest;
import com.familykitchen.dish.model.dto.DishTemplateRejectRequest;
import com.familykitchen.dish.model.vo.DishTemplateChangeDetailView;
import com.familykitchen.dish.model.vo.DishTemplateChangePageView;
import com.familykitchen.dish.service.DishTemplateChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 平台后台模板菜品修改审核接口。 */
@RestController
@RequestMapping("/admin/dish-template-change-requests")
@Tag(name = "平台后台-模板菜品修改审核", description = "查询、通过或驳回商户提交的模板菜品修改申请")
@SecurityRequirement(name = "bearerAuth")
public class AdminDishTemplateChangeRequestController {
  private final CurrentUserProvider currentUserProvider;
  private final DishTemplateChangeRequestService service;

  /**
   * 创建平台模板菜品修改审核控制器。
   * @param currentUserProvider 当前登录用户提供器
   * @param service 模板菜品修改申请服务
   */
  public AdminDishTemplateChangeRequestController(CurrentUserProvider currentUserProvider,
      DishTemplateChangeRequestService service) {
    this.currentUserProvider = currentUserProvider;
    this.service = service;
  }

  /**
   * 分页查询全部商户的模板菜品修改申请。
   * @param servletRequest 当前 HTTP 请求
   * @param status 审核状态
   * @param merchantId 商户 ID
   * @param templateId 模板菜品 ID
   * @param keyword 模板菜名关键词
   * @param page 页码
   * @param pageSize 每页数量
   * @return 全平台申请分页结果
   */
  @GetMapping
  @Operation(summary = "查询模板菜品修改申请", description = "平台管理员可按状态、商户、模板和菜名筛选。")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
  public ApiResponse<DishTemplateChangePageView> page(HttpServletRequest servletRequest,
      @RequestParam(required = false) String status, @RequestParam(required = false) Long merchantId,
      @RequestParam(required = false) Long templateId, @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer pageSize) {
    CurrentUserContext user = currentUserProvider.require(servletRequest);
    return ApiResponse.ok(service.adminPage(user,
        new AdminDishTemplateChangeQuery(status, merchantId, templateId, keyword, page, pageSize)));
  }

  /**
   * 查询单个模板菜品修改申请详情。
   * @param servletRequest 当前 HTTP 请求
   * @param requestId 修改申请 ID
   * @return 申请详情与双快照
   */
  @GetMapping("/{requestId}")
  @Operation(summary = "查询模板菜品修改申请详情")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
  public ApiResponse<DishTemplateChangeDetailView> detail(HttpServletRequest servletRequest,
      @Parameter(description = "修改申请ID", required = true) @PathVariable Long requestId) {
    return ApiResponse.ok(service.adminDetail(currentUserProvider.require(servletRequest), requestId));
  }

  /**
   * 审核通过并原子覆盖平台模板主信息和全部食材。
   * @param servletRequest 当前 HTTP 请求
   * @param requestId 修改申请 ID
   * @param request 可选审核意见
   * @return 空成功响应
   */
  @PostMapping("/{requestId}/approve")
  @Operation(summary = "通过模板菜品修改申请")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核通过")
  public ApiResponse<Void> approve(HttpServletRequest servletRequest,
      @Parameter(description = "修改申请ID", required = true) @PathVariable Long requestId,
      @Valid @RequestBody(required = false) DishTemplateApproveRequest request) {
    service.approve(currentUserProvider.require(servletRequest), requestId, request);
    return ApiResponse.ok();
  }

  /**
   * 驳回申请并向商户通知必填原因。
   * @param servletRequest 当前 HTTP 请求
   * @param requestId 修改申请 ID
   * @param request 必填驳回原因
   * @return 空成功响应
   */
  @PostMapping("/{requestId}/reject")
  @Operation(summary = "驳回模板菜品修改申请")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核驳回")
  public ApiResponse<Void> reject(HttpServletRequest servletRequest,
      @Parameter(description = "修改申请ID", required = true) @PathVariable Long requestId,
      @Valid @RequestBody DishTemplateRejectRequest request) {
    service.reject(currentUserProvider.require(servletRequest), requestId, request);
    return ApiResponse.ok();
  }
}
