package com.familykitchen.wallet.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.wallet.model.dto.AdjustFamilyBalanceRequest;
import com.familykitchen.wallet.model.vo.FamilyWalletLedgerView;
import com.familykitchen.wallet.model.vo.FamilyWalletSummaryView;
import com.familykitchen.wallet.service.FamilyWalletApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Merchant-facing family wallet endpoints. */
@RestController
@RequestMapping("/merchant/families/{familyId}/wallet")
@Tag(name = "商户端-家庭钱包", description = "商户查看并调整服务家庭的钱包")
public class MerchantFamilyWalletController {
  private final CurrentUserProvider users;
  private final FamilyWalletApplicationService wallets;
  /** Creates the merchant family wallet controller.
   * @param users current user provider
   * @param wallets wallet application service
   */
  public MerchantFamilyWalletController(
      CurrentUserProvider users, FamilyWalletApplicationService wallets) {
    this.users = users; this.wallets = wallets;
  }
  /** Returns a service family's wallet.
   * @param request HTTP request
   * @param familyId family
   * @return wallet
   */
  @GetMapping
  @Operation(summary = "查询家庭钱包")
  public ApiResponse<FamilyWalletSummaryView> summary(
      HttpServletRequest request, @PathVariable Long familyId) {
    return ApiResponse.ok(wallets.summaryForMerchant(merchant(request), familyId));
  }
  /** Returns one page of a service family's wallet ledgers.
   * @param request HTTP request
   * @param familyId family
   * @param page page
   * @param pageSize size
   * @return ledgers
   */
  @GetMapping("/ledgers")
  @Operation(summary = "查询家庭钱包流水")
  public ApiResponse<List<FamilyWalletLedgerView>> ledgers(HttpServletRequest request,
      @PathVariable Long familyId, @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(wallets.ledgersForMerchant(merchant(request), familyId, page, pageSize));
  }
  /** Applies an idempotent family-wallet adjustment.
   * @param request HTTP request
   * @param familyId family
   * @param body adjustment
   * @return wallet
   */
  @PostMapping("/adjust")
  @Operation(summary = "调整家庭钱包")
  public ApiResponse<FamilyWalletSummaryView> adjust(HttpServletRequest request,
      @PathVariable Long familyId, @Valid @RequestBody AdjustFamilyBalanceRequest body) {
    body.requestId();
    return ApiResponse.ok(wallets.adjustForMerchant(merchant(request), familyId, body));
  }
  private CurrentUserContext merchant(HttpServletRequest request) {
    CurrentUserContext user = users.require(request);
    if (!user.hasMerchantBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无商户后台访问权限");
    }
    return user;
  }
}
