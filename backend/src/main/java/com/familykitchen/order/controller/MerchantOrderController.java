package com.familykitchen.order.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.order.model.dto.CancelOrderRequest;
import com.familykitchen.order.model.dto.DeliveryFeeRequest;
import com.familykitchen.order.model.dto.OrderStatusRequest;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.service.MerchantOrderApplicationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户订单控制器。
 *
 * <p>提供商户后台确认、驳回、取消订单、调整配送费和推进订单状态的接口。</p>
 */
@RestController
@RequestMapping("/merchant/orders")
@Tag(name = "商户端-订单", description = "商户确认、取消、调价与推进订单状态接口")
public class MerchantOrderController {

  private final CurrentUserProvider currentUserProvider;
  private final MerchantOrderApplicationService merchantOrderApplicationService;

  /**
   * 创建商户订单实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param merchantOrderApplicationService 商户订单申请Service
   */
  public MerchantOrderController(
      CurrentUserProvider currentUserProvider,
      MerchantOrderApplicationService merchantOrderApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.merchantOrderApplicationService = merchantOrderApplicationService;
  }

  /**
   * 处理商户订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 列出的结果
   */
  @GetMapping
  @Operation(summary = "查询订单列表", description = "查询当前商户可处理的订单列表。")
  public ApiResponse<List<OrderView>> list(HttpServletRequest request) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantOrderApplicationService.list(user));
  }

  /**
   * 处理商户订单相关的 HTTP 请求。
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
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantOrderApplicationService.detail(user, orderId));
  }

  /**
   * 处理商户订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @return 处理的结果
   */
  @PostMapping("/{orderId}/confirm")
  @Operation(summary = "确认订单", description = "确认待处理订单。")
  public ApiResponse<OrderStatus> confirm(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantOrderApplicationService.confirm(user, orderId));
  }

  /**
   * 处理商户订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @return 拒绝的结果
   */
  @PostMapping("/{orderId}/reject")
  @Operation(summary = "驳回订单", description = "驳回待处理订单。")
  public ApiResponse<OrderStatus> reject(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantOrderApplicationService.reject(user, orderId));
  }

  /**
   * 处理商户订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @param body 请求体
   * @return 取消的结果
   */
  @PostMapping("/{orderId}/cancel")
  @Operation(summary = "取消订单", description = "商户取消指定订单。")
  public ApiResponse<OrderStatus> cancel(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId,
      @RequestBody(required = false) CancelOrderRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantOrderApplicationService.cancel(user, orderId, body == null ? null : body.reason()));
  }

  /**
   * 处理配送费用相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @param body 请求体
   * @return 处理配送费用的结果
   */
  @PostMapping("/{orderId}/delivery-fee")
  @Operation(summary = "调整配送费", description = "在订单确认前调整配送费。")
  public ApiResponse<Void> adjustDeliveryFee(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId,
      @Valid @RequestBody DeliveryFeeRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    merchantOrderApplicationService.adjustDeliveryFee(user, orderId, body.deliveryFee());
    return ApiResponse.ok();
  }

  /**
   * 处理商户订单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param orderId 订单标识
   * @param body 请求体
   * @return 处理的结果
   */
  @PostMapping("/{orderId}/status")
  @Operation(summary = "推进订单状态", description = "将订单推进到指定状态。")
  public ApiResponse<OrderStatus> advance(
      HttpServletRequest request,
      @Parameter(description = "订单 ID") @PathVariable Long orderId,
      @Valid @RequestBody OrderStatusRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantOrderApplicationService.advance(user, orderId, body));
  }

  private CurrentUserContext requireMerchant(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasMerchantBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无商户后台访问权限");
    }
    return user;
  }
}
