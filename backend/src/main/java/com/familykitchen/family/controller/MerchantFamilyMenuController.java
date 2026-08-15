package com.familykitchen.family.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.family.model.dto.CopyFamilyMenuRequest;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.service.MerchantFamilyMenuApplicationService;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
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
 * 商户家庭专属菜单控制器。
 *
 * <p>提供商户后台维护家庭专属菜单启用状态、排序和价格覆盖的接口。</p>
 */
@RestController
@RequestMapping("/merchant/families/{familyId}/menu")
@Tag(name = "商户端-家庭菜单", description = "商户维护家庭专属菜单配置接口")
public class MerchantFamilyMenuController {

  private final CurrentUserProvider currentUserProvider;
  private final MerchantFamilyMenuApplicationService merchantFamilyMenuApplicationService;

  /**
   * 创建商户家庭菜单实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param merchantFamilyMenuApplicationService 商户家庭菜单申请Service
   */
  public MerchantFamilyMenuController(
      CurrentUserProvider currentUserProvider,
      MerchantFamilyMenuApplicationService merchantFamilyMenuApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.merchantFamilyMenuApplicationService = merchantFamilyMenuApplicationService;
  }

  /**
   * 处理商户家庭菜单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @return 处理的结果
   */
  @GetMapping
  @Operation(summary = "获取家庭菜单", description = "返回指定家庭的专属菜单配置。")
  public ApiResponse<List<FamilyMenuItemView>> menu(HttpServletRequest request, @PathVariable Long familyId) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantFamilyMenuApplicationService.menu(user, familyId));
  }

  /**
   * 处理菜单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 保存菜单的结果
   */
  @PutMapping
  @Operation(summary = "保存家庭菜单", description = "保存家庭菜单的启用状态、排序和价格覆盖。")
  public ApiResponse<Void> saveMenu(
      HttpServletRequest request,
      @Parameter(description = "家庭 ID") @PathVariable Long familyId,
      @Valid @RequestBody SaveFamilyMenuRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    merchantFamilyMenuApplicationService.saveMenu(user, familyId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理菜单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 复制菜单的结果
   */
  @PostMapping("/copy")
  @Operation(summary = "复制家庭菜单", description = "从源家庭复制菜单配置到目标家庭。")
  public ApiResponse<Void> copyMenu(
      HttpServletRequest request,
      @Parameter(description = "目标家庭 ID") @PathVariable Long familyId,
      @Valid @RequestBody CopyFamilyMenuRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    merchantFamilyMenuApplicationService.copyMenu(user, familyId, body);
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
