package com.familykitchen.merchant.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.merchant.model.dto.UpdateMerchantProfileRequest;
import com.familykitchen.merchant.model.vo.MerchantProfileView;
import com.familykitchen.merchant.service.MerchantProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 商户负责人查看和修改自身商户业务资料的控制器。 */
@RestController
@RequestMapping("/merchant/profile")
@Tag(name = "商户端-商户资料", description = "商户负责人维护商户名称和联系资料")
public class MerchantProfileController {
  private final CurrentUserProvider currentUserProvider;
  private final MerchantProfileService merchantProfileService;

  /**
   * 创建商户资料控制器。
   * @param currentUserProvider 当前用户提供器
   * @param merchantProfileService 商户资料服务
   */
  public MerchantProfileController(CurrentUserProvider currentUserProvider,
      MerchantProfileService merchantProfileService) {
    this.currentUserProvider = currentUserProvider;
    this.merchantProfileService = merchantProfileService;
  }

  /**
   * 查询当前负责人所属商户的可编辑资料。
   * @param request HTTP 请求
   * @return 商户业务资料
   */
  @GetMapping
  @Operation(summary = "查询自身商户资料")
  public ApiResponse<MerchantProfileView> profile(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(merchantProfileService.profile(user));
  }

  /**
   * 更新当前负责人所属商户的可编辑资料。
   * @param request HTTP 请求
   * @param body 商户资料更新内容
   * @return 更新后的商户业务资料
   */
  @PutMapping
  @Operation(summary = "修改自身商户资料")
  public ApiResponse<MerchantProfileView> updateProfile(HttpServletRequest request,
      @Valid @RequestBody UpdateMerchantProfileRequest body) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(merchantProfileService.updateProfile(user, body));
  }
}
