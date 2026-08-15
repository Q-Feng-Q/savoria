package com.familykitchen.cart.controller;

import com.familykitchen.cart.model.dto.CartItemRequest;
import com.familykitchen.cart.model.dto.CartRemarkRequest;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.cart.service.CartApplicationService;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家庭餐篮控制器。
 *
 * <p>提供家庭端餐篮查询、加菜、改单项和备注维护接口，
 * 作为家庭点餐流程的直接入口。</p>
 */
@RestController
@RequestMapping("/family/cart")
@Tag(name = "家庭端-餐篮", description = "家庭成员餐篮查询与维护接口")
public class CartController {

  private final CurrentUserProvider currentUserProvider;
  private final CartApplicationService cartApplicationService;

  /**
   * 创建购物车实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param cartApplicationService 购物车申请Service
   */
  public CartController(CurrentUserProvider currentUserProvider, CartApplicationService cartApplicationService) {
    this.currentUserProvider = currentUserProvider;
    this.cartApplicationService = cartApplicationService;
  }

  /**
   * 处理购物车相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param mealSlotId mealSlot标识
   * @param date date
   * @return 处理的结果
   */
  @GetMapping
  @Operation(summary = "查询餐篮", description = "按餐次和日期查询当前成员的当日餐篮。")
  public ApiResponse<CartView> cart(
      HttpServletRequest request,
      @Parameter(description = "餐次 ID") @RequestParam Long mealSlotId,
      @Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(cartApplicationService.cart(user, mealSlotId, date));
  }

  /**
   * 处理项目相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 新增项目的结果
   */
  @PostMapping("/items")
  @Operation(summary = "新增餐篮项", description = "向当前餐篮中加入一道菜品。")
  public ApiResponse<CartView.CartItemView> addItem(
      HttpServletRequest request,
      @Valid @RequestBody CartItemRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(cartApplicationService.addItem(user, body));
  }

  /**
   * 处理项目相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param itemId 项目标识
   * @param body 请求体
   * @return 更新项目的结果
   */
  @PutMapping("/items/{itemId}")
  @Operation(summary = "更新餐篮项", description = "更新餐篮项数量、日期或备注。")
  public ApiResponse<Void> updateItem(
      HttpServletRequest request,
      @Parameter(description = "餐篮项 ID") @PathVariable Long itemId,
      @Valid @RequestBody CartItemRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    cartApplicationService.updateItem(user, itemId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理项目相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param itemId 项目标识
   * @return 删除项目的结果
   */
  @DeleteMapping("/items/{itemId}")
  @Operation(summary = "删除餐篮项", description = "删除指定餐篮项。")
  public ApiResponse<Void> deleteItem(HttpServletRequest request, @PathVariable Long itemId) {
    CurrentUserContext user = currentUserProvider.require(request);
    cartApplicationService.deleteItem(user, itemId);
    return ApiResponse.ok();
  }

  /**
   * 处理备注相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 更新备注的结果
   */
  @PutMapping("/remark")
  @Operation(summary = "更新餐篮备注", description = "更新当前餐篮的整体备注信息。")
  public ApiResponse<Void> updateRemark(
      HttpServletRequest request,
      @Valid @RequestBody CartRemarkRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    cartApplicationService.updateRemark(user, body);
    return ApiResponse.ok();
  }
}
