package com.familykitchen.dish.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.IngredientDictionaryRequest;
import com.familykitchen.dish.service.IngredientDictionaryApplicationService;
import com.familykitchen.dish.model.vo.IngredientDictionaryView;
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
 * 商户食材字典控制器。
 *
 * <p>提供食材字典的查询、新增、修改和删除能力，
 * 供菜品配方配置与采购推导复用。</p>
 */
@RestController
@RequestMapping("/merchant/ingredients")
@Tag(name = "商户端-食材字典", description = "商户食材字典维护接口")
public class IngredientDictionaryController {

  private final CurrentUserProvider currentUserProvider;
  private final IngredientDictionaryApplicationService ingredientDictionaryApplicationService;

  /**
   * 创建食材Dictionary实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param ingredientDictionaryApplicationService 食材Dictionary申请Service
   */
  public IngredientDictionaryController(
      CurrentUserProvider currentUserProvider,
      IngredientDictionaryApplicationService ingredientDictionaryApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.ingredientDictionaryApplicationService = ingredientDictionaryApplicationService;
  }

  /**
   * 处理食材Dictionary相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 列出的结果
   */
  @GetMapping
  @Operation(summary = "查询食材字典", description = "查询当前商户的食材字典列表。")
  public ApiResponse<List<IngredientDictionaryView>> list(HttpServletRequest request) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(ingredientDictionaryApplicationService.list(user));
  }

  /**
   * 处理食材Dictionary相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 创建的结果
   */
  @PostMapping
  @Operation(summary = "新增食材", description = "新增一个食材字典项。")
  public ApiResponse<IngredientDictionaryView> create(
      HttpServletRequest request,
      @Valid @RequestBody IngredientDictionaryRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(ingredientDictionaryApplicationService.create(user, body));
  }

  /**
   * 处理食材Dictionary相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param ingredientId 食材标识
   * @param body 请求体
   * @return 更新的结果
   */
  @PutMapping("/{ingredientId}")
  @Operation(summary = "更新食材", description = "更新指定食材字典项。")
  public ApiResponse<Void> update(
      HttpServletRequest request,
      @Parameter(description = "食材 ID") @PathVariable Long ingredientId,
      @Valid @RequestBody IngredientDictionaryRequest body
  ) {
    CurrentUserContext user = requireMerchant(request);
    ingredientDictionaryApplicationService.update(user, ingredientId, body);
    return ApiResponse.ok();
  }

  /**
   * 处理食材Dictionary相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param ingredientId 食材标识
   * @return 删除的结果
   */
  @DeleteMapping("/{ingredientId}")
  @Operation(summary = "删除食材", description = "删除指定食材字典项。")
  public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long ingredientId) {
    CurrentUserContext user = requireMerchant(request);
    ingredientDictionaryApplicationService.delete(user, ingredientId);
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
