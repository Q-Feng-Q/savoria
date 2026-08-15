package com.familykitchen.admin.controller;

import com.familykitchen.admin.model.dto.AdminFamilyMemberUpdateRequest;
import com.familykitchen.admin.model.dto.AdminFamilyUpdateRequest;
import com.familykitchen.admin.model.vo.AdminFamilyDetailView;
import com.familykitchen.admin.model.vo.AdminFamilyOptionView;
import com.familykitchen.admin.service.AdminFamilyService;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.family.model.dto.AddressRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.vo.WalletLedgerView;

/**
 * 提供平台管理家庭相关的 HTTP 接口，并将请求委托给应用服务。
 */
@RestController
@RequestMapping("/admin/families")
@Tag(name = "后台-平台家庭中心", description = "平台管理员跨商户管理家庭")
public class AdminFamilyController {
  private final CurrentUserProvider currentUserProvider;
  private final AdminFamilyService service;

  /**
   * 创建平台管理家庭实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param service service
   */
  public AdminFamilyController(CurrentUserProvider currentUserProvider, AdminFamilyService service) {
    this.currentUserProvider = currentUserProvider;
    this.service = service;
  }

  /**
   * 处理平台管理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 列出结果后的结果
   */
  @GetMapping
  @Operation(summary = "查询全部家庭")
  public ApiResponse<List<AdminFamilyOptionView>> list(HttpServletRequest request) {
    requirePlatformAdmin(request);
    return ApiResponse.ok(service.options());
  }

  /**
   * 处理平台管理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理结果
   */
  @GetMapping("/options")
  @Operation(summary = "查询家庭下拉选项")
  public ApiResponse<List<AdminFamilyOptionView>> options(HttpServletRequest request) {
    requirePlatformAdmin(request);
    return ApiResponse.ok(service.options());
  }

  /**
   * 处理平台管理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @return 处理结果
   */
  @GetMapping("/{familyId}")
  public ApiResponse<AdminFamilyDetailView> detail(HttpServletRequest request, @PathVariable Long familyId) {
    requirePlatformAdmin(request);
    return ApiResponse.ok(service.detail(familyId));
  }

  /**
   * 处理平台管理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 更新结果后的结果
   */
  @PutMapping("/{familyId}")
  public ApiResponse<Void> update(HttpServletRequest request, @PathVariable Long familyId,
                                  @Valid @RequestBody AdminFamilyUpdateRequest body) {
    requirePlatformAdmin(request);
    service.updateFamily(familyId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理平台管理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @return 停用结果后的结果
   */
  @DeleteMapping("/{familyId}")
  public ApiResponse<Void> disable(HttpServletRequest request, @PathVariable Long familyId) {
    requirePlatformAdmin(request);
    service.disableFamily(familyId);
    return ApiResponse.ok();
  }

  /**
   * 处理成员相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param body 请求体
   * @return 更新成员后的结果
   */
  @PutMapping("/{familyId}/members/{memberId}")
  public ApiResponse<Void> updateMember(HttpServletRequest request, @PathVariable Long familyId,
                                        @PathVariable Long memberId,
                                        @Valid @RequestBody AdminFamilyMemberUpdateRequest body) {
    requirePlatformAdmin(request);
    service.updateMember(familyId, memberId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理成员相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @return 停用成员后的结果
   */
  @DeleteMapping("/{familyId}/members/{memberId}")
  public ApiResponse<Void> disableMember(HttpServletRequest request, @PathVariable Long familyId,
                                         @PathVariable Long memberId) {
    requirePlatformAdmin(request);
    service.disableMember(familyId, memberId);
    return ApiResponse.ok();
  }

  /**
   * 处理地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 创建地址后的结果
   */
  @PostMapping("/{familyId}/addresses")
  public ApiResponse<Void> createAddress(HttpServletRequest request, @PathVariable Long familyId,
                                         @Valid @RequestBody AddressRequest body) {
    requirePlatformAdmin(request);
    service.createAddress(familyId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @param body 请求体
   * @return 更新地址后的结果
   */
  @PutMapping("/{familyId}/addresses/{addressId}")
  public ApiResponse<Void> updateAddress(HttpServletRequest request, @PathVariable Long familyId,
                                         @PathVariable Long addressId, @Valid @RequestBody AddressRequest body) {
    requirePlatformAdmin(request);
    service.updateAddress(familyId, addressId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @return 删除地址后的结果
   */
  @DeleteMapping("/{familyId}/addresses/{addressId}")
  public ApiResponse<Void> deleteAddress(HttpServletRequest request, @PathVariable Long familyId,
                                         @PathVariable Long addressId) {
    requirePlatformAdmin(request);
    service.deleteAddress(familyId, addressId);
    return ApiResponse.ok();
  }

  /**
   * 处理Default地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @return 设置Default地址后的结果
   */
  @PostMapping("/{familyId}/addresses/{addressId}/default")
  public ApiResponse<Void> setDefaultAddress(HttpServletRequest request, @PathVariable Long familyId,
                                             @PathVariable Long addressId) {
    requirePlatformAdmin(request);
    service.setDefaultAddress(familyId, addressId);
    return ApiResponse.ok();
  }

  /**
   * 处理平台管理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @return 处理结果
   */
  @GetMapping("/{familyId}/menu")
  public ApiResponse<List<FamilyMenuItemView>> menu(HttpServletRequest request, @PathVariable Long familyId) {
    CurrentUserContext user = requirePlatformAdmin(request);
    return ApiResponse.ok(service.menu(user, familyId));
  }

  /**
   * 处理菜单相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param body 请求体
   * @return 保存菜单后的结果
   */
  @PutMapping("/{familyId}/menu")
  public ApiResponse<Void> saveMenu(HttpServletRequest request, @PathVariable Long familyId,
                                    @Valid @RequestBody SaveFamilyMenuRequest body) {
    CurrentUserContext user = requirePlatformAdmin(request);
    service.saveMenu(user, familyId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理Ledgers相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @return 处理Ledgers后的结果
   */
  @GetMapping("/{familyId}/members/{memberId}/wallet/ledgers")
  public ApiResponse<List<WalletLedgerView>> walletLedgers(HttpServletRequest request,
      @PathVariable Long familyId, @PathVariable Long memberId) {
    CurrentUserContext user = requirePlatformAdmin(request);
    return ApiResponse.ok(service.walletLedgers(user, familyId, memberId));
  }

  /**
   * 处理余额相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param body 请求体
   * @return 处理余额后的结果
   */
  @PostMapping("/{familyId}/members/{memberId}/wallet/adjust")
  public ApiResponse<WalletLedgerView> adjustBalance(HttpServletRequest request,
      @PathVariable Long familyId, @PathVariable Long memberId,
      @Valid @RequestBody AdjustMemberBalanceRequest body) {
    CurrentUserContext user = requirePlatformAdmin(request);
    return ApiResponse.ok(service.adjustBalance(user, familyId, memberId, body));
  }

  private CurrentUserContext requirePlatformAdmin(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasPlatformBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无平台管理员权限");
    }
    return user;
  }
}
