package com.familykitchen.family.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.vo.DishDetailView;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.family.model.dto.AddressRequest;
import com.familykitchen.family.model.dto.FamilyApplyRequest;
import com.familykitchen.family.model.dto.JoinFamilyRequest;
import com.familykitchen.family.model.dto.DirectInvitationRequest;
import com.familykitchen.family.model.dto.MembershipDecisionRequest;
import com.familykitchen.family.model.dto.OwnerTransferRequest;
import com.familykitchen.family.model.entity.FamilyMembershipRequestDO;
import com.familykitchen.family.service.FamilyApplicationService;
import com.familykitchen.family.model.vo.FamilyOnboardingView;
import com.familykitchen.family.service.FamilyMemberApplicationService;
import com.familykitchen.family.model.vo.AddressView;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
 * 家庭前台控制器。
 *
 * <p>聚合家庭端首页、菜单、地址和钱包流水等前台常用接口。</p>
 */
@RestController
@RequestMapping("/family")
@Tag(name = "家庭端-家庭主页", description = "家庭首页、菜单、地址与钱包流水接口")
public class FamilyController {

  private final CurrentUserProvider currentUserProvider;
  private final FamilyApplicationService familyApplicationService;
  private final FamilyMemberApplicationService familyMemberApplicationService;

  /**
   * 创建家庭实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param familyApplicationService 家庭申请Service
   * @param familyMemberApplicationService 家庭成员申请Service
   */
  public FamilyController(
      CurrentUserProvider currentUserProvider,
      FamilyApplicationService familyApplicationService,
      FamilyMemberApplicationService familyMemberApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.familyApplicationService = familyApplicationService;
    this.familyMemberApplicationService = familyMemberApplicationService;
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理的结果
   */
  @GetMapping("/home")
  @Operation(summary = "获取家庭首页", description = "返回家庭小程序首页聚合数据。")
  public ApiResponse<FamilyHomeResponse> home(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.home(user));
  }

  /**
   * 处理Slots相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理Slots的结果
   */
  @GetMapping("/meal-slots")
  @Operation(summary = "获取家庭餐次", description = "返回当前家庭已启用的餐次选择项。")
  public ApiResponse<List<FamilyHomeResponse.MealSlotView>> mealSlots(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.mealSlots(user));
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param categoryId category标识
   * @param keyword keyword
   * @return 处理的结果
   */
  @GetMapping("/menu")
  @Operation(summary = "获取可点菜品", description = "返回当前家庭已启用菜单中的菜品列表。")
  public ApiResponse<List<DishView>> menu(
      HttpServletRequest request,
      @Parameter(description = "菜品分类 ID") @RequestParam(required = false) Long categoryId,
      @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.menuItems(user, categoryId, keyword));
  }

  /**
   * 处理详情相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param dishId 菜品标识
   * @return 处理详情的结果
   */
  @GetMapping("/menu/{dishId}")
  @Operation(summary = "获取菜品详情", description = "返回指定菜品的详细信息。")
  public ApiResponse<DishDetailView> dishDetail(
      HttpServletRequest request,
      @Parameter(description = "菜品 ID") @PathVariable Long dishId
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.dishDetail(user, dishId));
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理的结果
   */
  @GetMapping("/addresses")
  @Operation(summary = "获取地址列表", description = "返回当前家庭的收餐地址列表。")
  public ApiResponse<List<AddressView>> addresses(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.addresses(user));
  }

  /**
   * 处理地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 创建地址的结果
   */
  @PostMapping("/addresses")
  @Operation(summary = "新增地址", description = "为当前家庭新增一个配送地址。")
  public ApiResponse<AddressView> createAddress(
      HttpServletRequest request,
      @Valid @RequestBody AddressRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.createAddress(user, body));
  }

  /**
   * 处理地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param addressId 地址标识
   * @param body 请求体
   * @return 更新地址的结果
   */
  @PutMapping("/addresses/{addressId}")
  @Operation(summary = "更新地址", description = "更新指定地址的联系人和地址信息。")
  public ApiResponse<AddressView> updateAddress(
      HttpServletRequest request,
      @Parameter(description = "地址 ID") @PathVariable Long addressId,
      @Valid @RequestBody AddressRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.updateAddress(user, addressId, body));
  }

  /**
   * 处理Default地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param addressId 地址标识
   * @return 设置Default地址的结果
   */
  @PostMapping("/addresses/{addressId}/default")
  @Operation(summary = "设置默认地址", description = "将指定地址设置为当前家庭默认配送地址。")
  public ApiResponse<Void> setDefaultAddress(HttpServletRequest request, @PathVariable Long addressId) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyApplicationService.setDefaultAddress(user, addressId);
    return ApiResponse.ok();
  }

  /**
   * 处理地址相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param addressId 地址标识
   * @return 删除地址的结果
   */
  @DeleteMapping("/addresses/{addressId}")
  @Operation(summary = "删除地址", description = "删除指定配送地址。")
  public ApiResponse<Void> deleteAddress(HttpServletRequest request, @PathVariable Long addressId) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyApplicationService.deleteAddress(user, addressId);
    return ApiResponse.ok();
  }

  /**
   * 处理Ledgers相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理Ledgers的结果
   */
  @GetMapping("/me/wallet/ledgers")
  @Operation(summary = "获取钱包流水", description = "返回当前成员的钱包流水记录。")
  public ApiResponse<List<WalletLedgerView>> walletLedgers(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyApplicationService.walletLedgers(user));
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 提交家庭的结果
   */
  @PostMapping("/apply")
  @Operation(summary = "申请创建家庭", description = "提交家庭创建申请，等待平台审批。")
  public ApiResponse<Void> applyFamily(
      HttpServletRequest request,
      @Valid @RequestBody FamilyApplyRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyMemberApplicationService.applyFamily(user, body);
    return ApiResponse.ok();
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理的结果
   */
  @GetMapping("/onboarding")
  @Operation(summary = "查询当前用户的家庭引导状态")
  public ApiResponse<FamilyOnboardingView> onboarding(HttpServletRequest request) {
    return ApiResponse.ok(familyMemberApplicationService.onboarding(currentUserProvider.require(request)));
  }

  /**
   * 处理邀请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理邀请的结果
   */
  @PostMapping("/invite")
  @Operation(summary = "生成邀请码", description = "家庭管理员生成邀请码用于邀请新成员。")
  public ApiResponse<String> generateInvitation(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(familyMemberApplicationService.generateInvitation(user));
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 处理家庭的结果
   */
  @PostMapping("/join")
  @Operation(summary = "加入家庭", description = "通过邀请码加入家庭。")
  public ApiResponse<Void> joinFamily(
      HttpServletRequest request,
      @Valid @RequestBody JoinFamilyRequest body
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyMemberApplicationService.joinFamily(user, body);
    return ApiResponse.ok();
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理家庭的结果
   */
  @PostMapping("/exit")
  @Operation(summary = "退出当前家庭", description = "普通成员或管理员退出家庭；负责人需先转让或解散家庭。")
  public ApiResponse<Void> exitFamily(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    familyMemberApplicationService.exitFamily(user, "用户主动退出家庭");
    return ApiResponse.ok();
  }

  /**
   * 处理Invite相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 处理Invite的结果
   */
  @PostMapping("/invitations/direct")
  @Operation(summary="定向邀请已注册用户")
  public ApiResponse<Void> directInvite(HttpServletRequest request,@Valid @RequestBody DirectInvitationRequest body){
    familyMemberApplicationService.directInvite(currentUserProvider.require(request),body);return ApiResponse.ok();}

  /**
   * 处理Invitations相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理Invitations的结果
   */
  @GetMapping("/invitations/me")
  @Operation(summary="查询我的待处理家庭邀请")
  public ApiResponse<List<FamilyMembershipRequestDO>> myInvitations(HttpServletRequest request){
    return ApiResponse.ok(familyMemberApplicationService.myInvitations(currentUserProvider.require(request)));}

  /**
   * 处理邀请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @return 处理邀请的结果
   */
  @PostMapping("/invitations/{id}/accept")
  public ApiResponse<Void> acceptInvitation(HttpServletRequest request,@PathVariable Long id){
    familyMemberApplicationService.acceptInvitation(currentUserProvider.require(request),id);return ApiResponse.ok();}
  /**
   * 处理邀请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @param body 请求体
   * @return 拒绝邀请的结果
   */
  @PostMapping("/invitations/{id}/reject")
  public ApiResponse<Void> rejectInvitation(HttpServletRequest request,@PathVariable Long id,@RequestBody(required=false) MembershipDecisionRequest body){
    familyMemberApplicationService.rejectInvitation(currentUserProvider.require(request),id,body==null?null:body.reason());return ApiResponse.ok();}

  /**
   * 处理Applications相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理Applications的结果
   */
  @GetMapping("/join-applications")
  public ApiResponse<List<FamilyMembershipRequestDO>> pendingApplications(HttpServletRequest request){
    return ApiResponse.ok(familyMemberApplicationService.pendingApplications(currentUserProvider.require(request)));}
  /**
   * 处理申请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @return 批准申请的结果
   */
  @PostMapping("/join-applications/{id}/approve")
  public ApiResponse<Void> approveApplication(HttpServletRequest request,@PathVariable Long id){
    familyMemberApplicationService.approveApplication(currentUserProvider.require(request),id);return ApiResponse.ok();}
  /**
   * 处理申请相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @param body 请求体
   * @return 拒绝申请的结果
   */
  @PostMapping("/join-applications/{id}/reject")
  public ApiResponse<Void> rejectApplication(HttpServletRequest request,@PathVariable Long id,@RequestBody(required=false) MembershipDecisionRequest body){
    familyMemberApplicationService.rejectApplication(currentUserProvider.require(request),id,body==null?null:body.reason());return ApiResponse.ok();}

  /**
   * 处理负责人相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 移交负责人的结果
   */
  @PutMapping("/owner")
  public ApiResponse<Void> transferOwner(HttpServletRequest request,@Valid @RequestBody OwnerTransferRequest body){
    familyMemberApplicationService.transferOwner(currentUserProvider.require(request),body.targetUserId());return ApiResponse.ok();}
  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理家庭的结果
   */
  @DeleteMapping("/current")
  public ApiResponse<Void> dissolveFamily(HttpServletRequest request){
    familyMemberApplicationService.dissolveFamily(currentUserProvider.require(request));return ApiResponse.ok();}
}
