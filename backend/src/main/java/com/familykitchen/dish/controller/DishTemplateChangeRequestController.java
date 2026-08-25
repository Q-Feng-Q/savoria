package com.familykitchen.dish.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.DishTemplateChangeSubmitRequest;
import com.familykitchen.dish.model.dto.MerchantDishTemplateChangeQuery;
import com.familykitchen.dish.model.dto.ImportedDishTemplateSyncRequest;
import com.familykitchen.dish.model.vo.DishTemplateChangeDetailView;
import com.familykitchen.dish.model.vo.DishTemplateChangePageView;
import com.familykitchen.dish.model.vo.DishTemplateChangeSubmitView;
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

/** 商户后台模板菜品修改申请接口。 */
@RestController
@RequestMapping("/merchant")
@Tag(name = "商户端-模板菜品修改", description = "提交完整模板修改、查询进度和撤回待审核申请")
@SecurityRequirement(name = "bearerAuth")
public class DishTemplateChangeRequestController {
  private final CurrentUserProvider currentUserProvider;
  private final DishTemplateChangeRequestService service;

  /**
   * 创建商户模板菜品修改申请控制器。
   * @param currentUserProvider 当前登录用户提供器
   * @param service 模板菜品修改申请服务
   */
  public DishTemplateChangeRequestController(CurrentUserProvider currentUserProvider,
      DishTemplateChangeRequestService service) {
    this.currentUserProvider = currentUserProvider;
    this.service = service;
  }

  /**
   * 提交平台模板菜品完整修改申请。
   * @param servletRequest 当前 HTTP 请求
   * @param templateId 平台模板菜品 ID
   * @param request 完整目标快照和提交说明
   * @return 新建申请结果
   */
  @PostMapping("/dish-templates/{templateId}/change-requests")
  @Operation(summary = "提交模板菜品修改申请", description = "完整快照需经平台管理员审核，通过前不会修改模板。")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申请提交成功")
  public ApiResponse<DishTemplateChangeSubmitView> submit(HttpServletRequest servletRequest,
      @Parameter(description = "平台模板菜品ID", required = true) @PathVariable Long templateId,
      @Valid @RequestBody DishTemplateChangeSubmitRequest request) {
    return ApiResponse.ok(service.submit(currentUserProvider.require(servletRequest), templateId, request));
  }

  /**
   * 使用当前商户已导入菜品的实时资料提交来源模板修改申请。
   *
   * @param servletRequest 当前 HTTP 请求
   * @param dishId 当前商户菜品 ID
   * @param request 可选提交说明
   * @return 新建申请结果
   */
  @PostMapping("/dishes/{dishId}/template-change-requests")
  @Operation(summary = "将已导入菜品申请同步到模板",
      description = "服务端读取当前菜品资料生成完整快照，经平台审核通过后才更新来源模板；制作步骤不会同步。")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申请提交成功")
  public ApiResponse<DishTemplateChangeSubmitView> submitFromImportedDish(
      HttpServletRequest servletRequest,
      @Parameter(description = "当前商户菜品ID", required = true) @PathVariable Long dishId,
      @Valid @RequestBody ImportedDishTemplateSyncRequest request) {
    return ApiResponse.ok(service.submitFromImportedDish(
        currentUserProvider.require(servletRequest), dishId, request));
  }

  /**
   * 分页查询当前商户提交的申请。
   * @param servletRequest 当前 HTTP 请求
   * @param status 审核状态
   * @param keyword 模板菜名关键词
   * @param page 页码
   * @param pageSize 每页数量
   * @return 当前商户申请分页结果
   */
  @GetMapping("/dish-template-change-requests")
  @Operation(summary = "查询商户模板修改申请", description = "只返回当前登录商户的申请。")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
  public ApiResponse<DishTemplateChangePageView> page(HttpServletRequest servletRequest,
      @RequestParam(required = false) String status, @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer pageSize) {
    CurrentUserContext user = currentUserProvider.require(servletRequest);
    return ApiResponse.ok(service.merchantPage(user,
        new MerchantDishTemplateChangeQuery(status, keyword, page, pageSize)));
  }

  /**
   * 查询当前商户申请详情与提交前后快照。
   * @param servletRequest 当前 HTTP 请求
   * @param requestId 修改申请 ID
   * @return 申请详情
   */
  @GetMapping("/dish-template-change-requests/{requestId}")
  @Operation(summary = "查询商户模板修改申请详情")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
  public ApiResponse<DishTemplateChangeDetailView> detail(HttpServletRequest servletRequest,
      @Parameter(description = "修改申请ID", required = true) @PathVariable Long requestId) {
    return ApiResponse.ok(service.merchantDetail(currentUserProvider.require(servletRequest), requestId));
  }

  /**
   * 撤回当前商户仍处于待审核状态的申请。
   * @param servletRequest 当前 HTTP 请求
   * @param requestId 修改申请 ID
   * @return 空成功响应
   */
  @PostMapping("/dish-template-change-requests/{requestId}/withdraw")
  @Operation(summary = "撤回模板菜品修改申请")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "撤回成功")
  public ApiResponse<Void> withdraw(HttpServletRequest servletRequest,
      @Parameter(description = "修改申请ID", required = true) @PathVariable Long requestId) {
    service.withdraw(currentUserProvider.require(servletRequest), requestId);
    return ApiResponse.ok();
  }
}
