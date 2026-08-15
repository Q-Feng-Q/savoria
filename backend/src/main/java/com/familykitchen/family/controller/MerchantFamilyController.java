package com.familykitchen.family.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.family.model.dto.UpdateFamilyDeliveryPolicyRequest;
import com.familykitchen.family.model.dto.UpdateMerchantFamilyProfileRequest;
import com.familykitchen.family.service.MerchantFamilyApplicationService;
import com.familykitchen.family.model.vo.MerchantFamilyDetailView;
import com.familykitchen.family.model.vo.MerchantFamilySummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户家庭档案控制器。
 *
 * <p>提供商户后台查看和维护家庭基础资料、配送策略等管理接口。</p>
 */
@RestController
@RequestMapping("/merchant/families")
@Tag(name = "商户端-家庭档案", description = "商户维护家庭档案与配送策略接口")
public class MerchantFamilyController {

  private final CurrentUserProvider currentUserProvider;
  private final MerchantFamilyApplicationService merchantFamilyApplicationService;

  /**
   * 创建商户家庭实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param merchantFamilyApplicationService 商户家庭申请Service
   */
  public MerchantFamilyController(
      CurrentUserProvider currentUserProvider,
      MerchantFamilyApplicationService merchantFamilyApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.merchantFamilyApplicationService = merchantFamilyApplicationService;
  }

  /**
   * 处理商户家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 列出的结果
   */
  @GetMapping
  @Operation(summary = "查询家庭列表", description = "返回当前商户可管理的家庭列表。")
  public ApiResponse<List<MerchantFamilySummaryView>> list(HttpServletRequest request) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantFamilyApplicationService.listFamilies(user));
  }

  /**
   * 处理商户家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @return 处理的结果
   */
  @GetMapping("/{familyId}")
  @Operation(summary = "获取家庭详情", description = "返回指定家庭的详细档案信息。")
  public ApiResponse<MerchantFamilyDetailView> detail(HttpServletRequest request, @PathVariable Long familyId) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantFamilyApplicationService.detail(user, familyId));
  }

  /**
   * 处理资料相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 更新资料的结果
   */
  @PutMapping("/{familyId}/profile")
  @Operation(summary = "更新家庭档案", description = "更新家庭名称、备注和联系人等基础资料。")
  public ApiResponse<Void> updateProfile(
      HttpServletRequest request,
      @Parameter(description = "家庭 ID") @PathVariable Long familyId,
      @Valid @RequestBody UpdateMerchantFamilyProfileRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    merchantFamilyApplicationService.updateProfile(user, familyId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理配送Policy相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 更新配送Policy的结果
   */
  @PutMapping("/{familyId}/delivery-policy")
  @Operation(summary = "更新配送策略", description = "更新家庭默认配送费和是否支持配送。")
  public ApiResponse<Void> updateDeliveryPolicy(
      HttpServletRequest request,
      @Parameter(description = "家庭 ID") @PathVariable Long familyId,
      @Valid @RequestBody UpdateFamilyDeliveryPolicyRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    merchantFamilyApplicationService.updateDeliveryPolicy(user, familyId, body);
    return ApiResponse.ok();
  }

  private CurrentUserContext requireMerchant(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasMerchantBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无商户后台访问权限");
    }
    return user;
  }
}
