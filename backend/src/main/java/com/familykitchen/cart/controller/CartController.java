package com.familykitchen.cart.controller;

import com.familykitchen.cart.model.dto.CartMutationRequest;
import com.familykitchen.cart.model.dto.CartRemarkRequest;
import com.familykitchen.cart.model.dto.ExpectedMealTimeRequest;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.cart.service.CartApplicationService;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP API for the single active shared family cart. */
@RestController
@RequestMapping("/family/cart")
@Tag(name = "家庭端-餐篮", description = "家庭共享餐篮查询与维护接口")
public class CartController {
  private final CurrentUserProvider currentUserProvider;
  private final CartApplicationService carts;

  /**
   * Creates the controller.
   *
   * @param currentUserProvider current-user provider
   * @param carts cart service
   */
  public CartController(CurrentUserProvider currentUserProvider, CartApplicationService carts) {
    this.currentUserProvider = currentUserProvider;
    this.carts = carts;
  }

  /**
   * Returns the active family cart without legacy date or meal-slot parameters.
   *
   * @param request HTTP request
   * @return authoritative cart view
   */
  @GetMapping
  @Operation(summary = "查询家庭共享餐篮")
  public ApiResponse<CartView> cart(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(carts.cart(user));
  }

  /**
   * Sets one dish's absolute quantity for the current member.
   *
   * @param request HTTP request
   * @param body versioned mutation
   * @return authoritative cart view
   */
  @PutMapping("/items")
  @Operation(summary = "修改当前成员的菜品数量")
  public ApiResponse<CartView> mutateItem(
      HttpServletRequest request, @Valid @RequestBody CartMutationRequest body) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(carts.mutateItem(user, body));
  }

  /**
   * Updates today's expected meal time.
   *
   * @param request HTTP request
   * @param body versioned mutation
   * @return authoritative cart view
   */
  @PutMapping("/expected-meal-time")
  @Operation(summary = "修改预计用餐时间")
  public ApiResponse<CartView> updateExpectedMealTime(
      HttpServletRequest request, @Valid @RequestBody ExpectedMealTimeRequest body) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(carts.updateExpectedMealTime(user, body));
  }

  /**
   * Updates the shared cart remark.
   *
   * @param request HTTP request
   * @param body versioned mutation
   * @return authoritative cart view
   */
  @PutMapping("/remark")
  @Operation(summary = "修改共享餐篮备注")
  public ApiResponse<CartView> updateRemark(
      HttpServletRequest request, @Valid @RequestBody CartRemarkRequest body) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(carts.updateRemark(user, body));
  }
}
