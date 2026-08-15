package com.familykitchen.wallet.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import com.familykitchen.wallet.service.MerchantWalletApplicationService;
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
 * 商户成员钱包控制器。
 *
 * <p>提供商户后台查看成员钱包流水、人工调整余额等管理能力。</p>
 */
@RestController
@RequestMapping("/merchant/members")
@Tag(name = "商户端-钱包", description = "商户查看成员钱包流水与调账接口")
public class MerchantWalletController {

  private final CurrentUserProvider currentUserProvider;
  private final MerchantWalletApplicationService merchantWalletApplicationService;

  /**
   * 创建商户钱包实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param merchantWalletApplicationService 商户钱包申请Service
   */
  public MerchantWalletController(
      CurrentUserProvider currentUserProvider,
      MerchantWalletApplicationService merchantWalletApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.merchantWalletApplicationService = merchantWalletApplicationService;
  }

  /**
   * 处理Ledgers相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param memberId 成员标识
   * @return 处理Ledgers的结果
   */
  @GetMapping("/{memberId}/wallet/ledgers")
  @Operation(summary = "查询钱包流水", description = "查询指定成员的钱包流水记录。")
  public ApiResponse<List<WalletLedgerView>> walletLedgers(
      HttpServletRequest request,
      @Parameter(description = "成员 ID") @PathVariable Long memberId
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantWalletApplicationService.walletLedgers(user, memberId));
  }

  /**
   * 处理余额相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param memberId 成员标识
   * @param body 请求体
   * @return 处理余额的结果
   */
  @PostMapping("/{memberId}/wallet/adjust")
  @Operation(summary = "调整钱包余额", description = "按类型对指定成员的钱包余额执行加款或扣款。")
  public ApiResponse<WalletLedgerView> adjustBalance(
      HttpServletRequest request,
      @Parameter(description = "成员 ID") @PathVariable Long memberId,
      @Valid @RequestBody AdjustMemberBalanceRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(merchantWalletApplicationService.adjustBalance(user, memberId, body));
  }

  private CurrentUserContext requireMerchant(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasMerchantBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无商户后台访问权限");
    }
    return user;
  }
}
