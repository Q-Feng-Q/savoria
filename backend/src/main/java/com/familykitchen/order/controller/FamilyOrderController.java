package com.familykitchen.order.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.order.model.dto.CancelOrderRequest;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.service.FamilyOrderApplicationService;
import com.familykitchen.order.model.vo.OrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家庭订单控制器。
 *
 * <p>提供家庭端提交订单、更新订单、取消订单以及查询订单的接口。</p>
 */
@RestController
@RequestMapping("/family/orders")
@Tag(name = "家庭端-订单", description = "家庭提交、更新、取消与查询订单接口")
public class FamilyOrderController {

  private final CurrentUserProvider currentUserProvider;
  private final FamilyOrderApplicationService familyOrderApplicationService;

  /**
   * 创建家庭订单实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param familyOrderApplicationService 家庭订单申请Service
   */
  public FamilyOrderController(
      CurrentUserProvider currentUserProvider,
      FamilyOrderApplicationService familyOrderApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.familyOrderApplicationService = familyOrderApplicationService;
  }

  /**
   * 处理家庭订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 提交的结果
   */
  @PostMapping
  @Operation(summary = "提交订单", description = "基于当前成员餐篮提交订单。")
  public ApiResponse<OrderView> submit(
      HttpServletRequest request,
      @Valid @RequestBody SubmitOrderRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyOrderApplicationService.submit(user, body));
  }

  /**
   * 处理家庭订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @return 更新的结果
   */
  @PutMapping("/{orderId}")
  @Operation(summary = "更新订单", description = "在待处理阶段按当前餐篮内容重算订单。")
  public ApiResponse<Void> update(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyOrderApplicationService.update(user, orderId);
    return ApiResponse.ok();
  }

  /**
   * 处理家庭订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @param body 请求体
   * @return 取消的结果
   */
  @PostMapping("/{orderId}/cancel")
  @Operation(summary = "取消订单", description = "取消当前家庭的指定订单。")
  public ApiResponse<Void> cancel(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId,
      @RequestBody(required = false) CancelOrderRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyOrderApplicationService.cancel(user, orderId, body == null ? null : body.reason());
    return ApiResponse.ok();
  }

  /**
   * 处理家庭订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 列出的结果
   */
  @GetMapping
  @Operation(summary = "查询订单列表", description = "查询当前家庭的订单列表。")
  public ApiResponse<List<OrderView>> list(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyOrderApplicationService.list(user));
  }

  /**
   * 处理家庭订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @return 处理的结果
   */
  @GetMapping("/{orderId}")
  @Operation(summary = "查询订单详情", description = "查询指定订单的详情。")
  public ApiResponse<OrderView> detail(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyOrderApplicationService.detail(user, orderId));
  }
}
