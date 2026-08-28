package com.familykitchen.wallet.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.wallet.model.vo.FamilyWalletLedgerView;
import com.familykitchen.wallet.model.vo.FamilyWalletSummaryView;
import com.familykitchen.wallet.service.FamilyWalletApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Family-facing shared wallet endpoints. */
@RestController
@RequestMapping("/family/wallet")
@Tag(name = "家庭端-家庭钱包", description = "家庭共享余额与流水")
public class FamilyWalletController {
  private final CurrentUserProvider users;
  private final FamilyWalletApplicationService wallets;
  /** Creates the family wallet controller.
   * @param users current user provider
   * @param wallets wallet application service
   */
  public FamilyWalletController(CurrentUserProvider users, FamilyWalletApplicationService wallets) {
    this.users = users; this.wallets = wallets;
  }
  /** Returns the current family's shared wallet.
   * @param request HTTP request
   * @return shared wallet summary
   */
  @GetMapping
  @Operation(summary = "查询家庭钱包")
  public ApiResponse<FamilyWalletSummaryView> summary(HttpServletRequest request) {
    return ApiResponse.ok(wallets.summary(users.require(request)));
  }
  /** Returns one page of current-family wallet ledgers.
   * @param request HTTP request
   * @param page page
   * @param pageSize size
   * @return wallet ledgers
   */
  @GetMapping("/ledgers")
  @Operation(summary = "查询家庭钱包流水")
  public ApiResponse<List<FamilyWalletLedgerView>> ledgers(HttpServletRequest request,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(wallets.ledgers(users.require(request), page, pageSize));
  }
}
