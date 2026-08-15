package com.familykitchen.dish.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.DishCategoryRequest;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.dto.DishStatusRequest;
import com.familykitchen.dish.model.dto.DishMutationResult;
import com.familykitchen.dish.model.dto.DishRequest.CookingStepRequest;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.model.vo.DishCategoryView;
import com.familykitchen.dish.model.vo.DishDetailView;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.dish.model.entity.DishReviewSubmissionDO;
import com.familykitchen.dish.service.DishReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * 商户菜品控制器。
 *
 * <p>提供商户后台的菜品、制作步骤和菜品分类管理接口。</p>
 */
@RestController
@RequestMapping("/merchant")
@Tag(name = "商户端-菜品", description = "商户菜品、制作步骤与分类管理接口")
public class DishController {

  private final CurrentUserProvider currentUserProvider;
  private final DishApplicationService dishApplicationService;
  private final DishReviewService dishReviewService;

  /**
   * 创建菜品实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param dishApplicationService 菜品申请Service
   * @param dishReviewService 菜品审核Service
   */
  public DishController(CurrentUserProvider currentUserProvider, DishApplicationService dishApplicationService,
                        DishReviewService dishReviewService) {
    this.currentUserProvider = currentUserProvider;
    this.dishApplicationService = dishApplicationService;
    this.dishReviewService = dishReviewService;
  }

  /**
   * 处理History相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理History的结果
   */
  @GetMapping("/dish-reviews")
  @Operation(summary = "查询本商户菜品审核历史")
  public ApiResponse<List<DishReviewSubmissionDO>> reviewHistory(HttpServletRequest request){
    CurrentUserContext user=requireMerchant(request);return ApiResponse.ok(dishReviewService.merchantHistory(user.merchantId()));}

  /**
   * 处理详情相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param reviewId 审核标识
   * @return 处理详情的结果
   */
  @GetMapping("/dish-reviews/{reviewId}")
  @Operation(summary = "查询本商户菜品审核详情")
  public ApiResponse<DishReviewSubmissionDO> reviewDetail(HttpServletRequest request,@PathVariable Long reviewId){
    CurrentUserContext user=requireMerchant(request);return ApiResponse.ok(dishReviewService.merchantDetail(user.merchantId(),reviewId));}

  /**
   * 处理审核相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param reviewId 审核标识
   * @return 处理审核的结果
   */
  @PostMapping("/dish-reviews/{reviewId}/withdraw")
  @Operation(summary = "撤回待审核菜品")
  public ApiResponse<Void> withdrawReview(HttpServletRequest request,@PathVariable Long reviewId){
    CurrentUserContext user=requireMerchant(request);dishReviewService.withdraw(user.merchantId(),reviewId,user.userId());return ApiResponse.ok();}

  /**
   * 处理菜品相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理的结果
   */
  @GetMapping("/dishes")
  @Operation(summary = "查询菜品列表", description = "查询当前商户可管理的菜品列表。")
  public ApiResponse<List<DishView>> dishes(HttpServletRequest request) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(dishApplicationService.dishes(user));
  }

  /**
   * 处理详情相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param dishId 菜品标识
   * @return 处理详情的结果
   */
  @GetMapping("/dishes/{dishId}")
  @Operation(summary = "查询菜品详情", description = "查询指定菜品的完整详情。")
  public ApiResponse<DishDetailView> dishDetail(
      HttpServletRequest request,
      @Parameter(description = "菜品 ID") @PathVariable Long dishId
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(dishApplicationService.detail(user, dishId));
  }

  /**
   * 处理菜品相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 创建菜品的结果
   */
  @PostMapping("/dishes")
  @Operation(summary = "新增菜品", description = "新增一个商户菜品。")
  public ApiResponse<DishView> createDish(HttpServletRequest request, @Valid @RequestBody DishRequest body) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(dishApplicationService.createDish(user, body));
  }

  /**
   * 处理菜品相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param dishId 菜品标识
   * @param body 请求体
   * @return 更新菜品的结果
   */
  @PutMapping("/dishes/{dishId}")
  @Operation(summary = "更新菜品", description = "更新指定菜品的基础资料。")
  public ApiResponse<DishMutationResult> updateDish(
      HttpServletRequest request,
      @Parameter(description = "菜品 ID") @PathVariable Long dishId,
      @Valid @RequestBody DishRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(dishApplicationService.updateDish(user, dishId, body));
  }

  /** Updates one dish availability status.
   * @param request servlet request carrying the authenticated identity
   * @param dishId dish identifier
   * @param body canonical status request
   * @return empty success response
   */
  @PutMapping("/dishes/{dishId}/status")
  @Operation(summary = "更新菜品状态")
  public ApiResponse<DishMutationResult> updateDishStatus(HttpServletRequest request, @PathVariable Long dishId,
                                             @Valid @RequestBody DishStatusRequest body) {
    return ApiResponse.ok(dishApplicationService.updateDishStatus(requireMerchant(request), dishId, body));
  }

  /**
   * 处理CookingSteps相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param dishId 菜品标识
   * @param body 请求体
   * @return 更新CookingSteps的结果
   */
  @PutMapping("/dishes/{dishId}/cooking-steps")
  @Operation(summary = "更新制作步骤", description = "更新指定菜品的制作步骤。")
  public ApiResponse<Void> updateCookingSteps(
      HttpServletRequest request,
      @Parameter(description = "菜品 ID") @PathVariable Long dishId,
      @Valid @RequestBody List<@Valid CookingStepRequest> body
  ) {
    CurrentUserContext user = requireMerchant(request);
    dishApplicationService.updateCookingSteps(user, dishId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理菜品相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理的结果
   */
  @GetMapping("/dish-categories")
  @Operation(summary = "查询菜品分类", description = "查询当前商户的菜品分类列表。")
  public ApiResponse<List<DishCategoryView>> categories(HttpServletRequest request) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(dishApplicationService.categories(user));
  }

  /**
   * 处理Category相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 创建Category的结果
   */
  @PostMapping("/dish-categories")
  @Operation(summary = "新增菜品分类", description = "新增一个菜品分类。")
  public ApiResponse<DishCategoryView> createCategory(
      HttpServletRequest request,
      @Valid @RequestBody DishCategoryRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(dishApplicationService.createCategory(user, body));
  }

  /**
   * 处理Category相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param categoryId category标识
   * @param body 请求体
   * @return 更新Category的结果
   */
  @PutMapping("/dish-categories/{categoryId}")
  @Operation(summary = "更新菜品分类", description = "更新指定菜品分类。")
  public ApiResponse<Void> updateCategory(
      HttpServletRequest request,
      @Parameter(description = "分类 ID") @PathVariable Long categoryId,
      @Valid @RequestBody DishCategoryRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    dishApplicationService.updateCategory(user, categoryId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理Category相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param categoryId category标识
   * @return 删除Category的结果
   */
  @DeleteMapping("/dish-categories/{categoryId}")
  @Operation(summary = "删除菜品分类", description = "删除指定菜品分类。")
  public ApiResponse<Void> deleteCategory(
      HttpServletRequest request,
      @Parameter(description = "分类 ID") @PathVariable Long categoryId
  ) {
    CurrentUserContext user = requireMerchant(request);
    dishApplicationService.deleteCategory(user, categoryId);
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
