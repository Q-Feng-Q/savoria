package com.familykitchen.purchase.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.purchase.model.dto.CheckedRequest;
import com.familykitchen.purchase.model.dto.TempPurchaseItemRequest;
import com.familykitchen.purchase.service.PurchaseApplicationService;
import com.familykitchen.purchase.model.vo.CheckoutPurchaseItemView;
import com.familykitchen.purchase.model.vo.PurchaseItemSummary;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户采购控制器。
 *
 * <p>提供采购汇总、按家庭查看采购项、临时采购项维护和采购文本复制接口。</p>
 */
@RestController
@RequestMapping("/merchant/purchases")
@Tag(name = "商户端-采购", description = "商户采购汇总与临时采购项维护接口")
public class PurchaseController {

  private final CurrentUserProvider currentUserProvider;
  private final PurchaseApplicationService purchaseApplicationService;

  /**
   * 创建采购实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param purchaseApplicationService 采购申请Service
   */
  public PurchaseController(
      CurrentUserProvider currentUserProvider,
      PurchaseApplicationService purchaseApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.purchaseApplicationService = purchaseApplicationService;
  }

  /**
   * 处理采购相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param date date
   * @param includePending includePending
   * @return 处理的结果
   */
  @GetMapping("/summary")
  @Operation(summary = "查询采购汇总", description = "按日期汇总商户采购清单。")
  public ApiResponse<List<PurchaseItemSummary>> summary(
      HttpServletRequest request,
      @Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Parameter(description = "是否包含待确认订单的预估采购项") @RequestParam(defaultValue = "true")
      boolean includePending
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(purchaseApplicationService.summary(user.merchantId(), date, includePending));
  }

  /**
   * 处理家庭相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param familyId 家庭标识
   * @param date date
   * @param includePending includePending
   * @return 处理家庭的结果
   */
  @GetMapping("/by-family")
  @Operation(summary = "按家庭查看采购项", description = "查看指定家庭对应的采购明细。")
  public ApiResponse<List<CheckoutPurchaseItemView>> byFamily(
      HttpServletRequest request,
      @Parameter(description = "家庭 ID") @RequestParam Long familyId,
      @Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Parameter(description = "是否包含待确认订单的预估采购项") @RequestParam(defaultValue = "true")
      boolean includePending
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(purchaseApplicationService.byFamily(user.merchantId(), familyId, date, includePending));
  }

  /**
   * 处理采购相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param itemId 项目标识
   * @param body 请求体
   * @return 处理的结果
   */
  @PostMapping("/items/{itemId}/checked")
  @Operation(summary = "勾选采购项", description = "更新临时采购项的勾选状态。")
  public ApiResponse<Void> checked(
      HttpServletRequest request,
      @Parameter(description = "采购项 ID") @PathVariable Long itemId,
      @Valid @RequestBody CheckedRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    purchaseApplicationService.checked(user.merchantId(), itemId, body.checked());
    return ApiResponse.ok();
  }

  /**
   * 处理Temp项目相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 新增Temp项目的结果
   */
  @PostMapping("/temp-items")
  @Operation(summary = "新增临时采购项", description = "新增一个不来自订单推导的临时采购项。")
  public ApiResponse<TempPurchaseItemRequest> addTempItem(
      HttpServletRequest request,
      @Valid @RequestBody TempPurchaseItemRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(purchaseApplicationService.addTempItem(user.merchantId(), body));
  }

  /**
   * 查询商户指定日期的全部临时采购项。
   *
   * @param request 请求参数
   * @param date 服务日期
   * @return 临时采购项
   */
  @GetMapping("/temp-items")
  @Operation(summary = "查询临时采购项", description = "查询指定日期下商户维护的全部临时采购项。")
  public ApiResponse<List<CheckoutPurchaseItemView>> tempItems(
      HttpServletRequest request,
      @Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(purchaseApplicationService.tempItems(user.merchantId(), date));
  }

  /**
   * 处理Temp项目相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param itemId 项目标识
   * @return 删除Temp项目的结果
   */
  @DeleteMapping("/temp-items/{itemId}")
  @Operation(summary = "删除临时采购项", description = "删除指定临时采购项。")
  public ApiResponse<Void> deleteTempItem(HttpServletRequest request, @PathVariable Long itemId) {
    CurrentUserContext user = requireMerchant(request);
    purchaseApplicationService.deleteTempItem(user.merchantId(), itemId);
    return ApiResponse.ok();
  }

  /**
   * 处理Text相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param date date
   * @param mealSlotId mealSlot标识
   * @return 复制Text的结果
   */
  @GetMapping("/copy-text")
  @Operation(summary = "复制采购文本", description = "生成可复制的采购清单文本。")
  public ApiResponse<String> copyText(
      HttpServletRequest request,
      @Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Parameter(description = "可选餐次 ID") @RequestParam(required = false) Long mealSlotId
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(purchaseApplicationService.copyText(user.merchantId(), date, mealSlotId));
  }

  private CurrentUserContext requireMerchant(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasMerchantBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无商户后台访问权限");
    }
    return user;
  }
}
