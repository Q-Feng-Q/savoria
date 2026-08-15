package com.familykitchen.dish.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.DishTemplateImportRequest;
import com.familykitchen.dish.model.dto.DishTemplateQuery;
import com.familykitchen.dish.model.vo.DishTemplateCategoryView;
import com.familykitchen.dish.model.vo.DishTemplateDetailView;
import com.familykitchen.dish.model.vo.DishTemplateImportResultView;
import com.familykitchen.dish.model.vo.DishTemplatePageView;
import com.familykitchen.dish.service.DishTemplateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 商户端平台菜品模板市场接口。 */
@RestController
@RequestMapping("/merchant")
@Tag(name = "商户端-菜品模板", description = "浏览平台家常菜模板并选择性导入到当前商户")
public class DishTemplateController {
  private final CurrentUserProvider currentUserProvider;
  private final DishTemplateService templateService;

  /**
   * 创建模板菜市场控制器。
   * @param currentUserProvider 当前登录用户解析器
   * @param templateService 模板菜品业务服务
   */
  public DishTemplateController(CurrentUserProvider currentUserProvider, DishTemplateService templateService) {
    this.currentUserProvider = currentUserProvider;
    this.templateService = templateService;
  }

  /**
   * 查询平台启用的模板分类。
   * @param request 当前 HTTP 请求
   * @return 模板分类列表
   */
  @GetMapping("/dish-template-categories")
  @Operation(summary = "查询模板分类", description = "返回平台当前启用的家常菜模板分类。")
  public ApiResponse<List<DishTemplateCategoryView>> categories(HttpServletRequest request) {
    requireMerchant(request);
    return ApiResponse.ok(templateService.categories());
  }

  /**
   * 分页查询模板菜品。
   * @param request 当前 HTTP 请求
   * @param categoryId 模板分类 ID
   * @param keyword 菜名关键词
   * @param imported 当前商户导入状态
   * @param page 页码
   * @param pageSize 每页数量
   * @return 模板分页结果
   */
  @GetMapping("/dish-templates")
  @Operation(summary = "分页查询模板菜品", description = "可按分类、菜名和当前商户导入状态筛选。")
  public ApiResponse<DishTemplatePageView> templates(HttpServletRequest request,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean imported,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer pageSize) {
    CurrentUserContext user = requireMerchant(request);
    return ApiResponse.ok(templateService.page(user,
        new DishTemplateQuery(categoryId, keyword, imported, page, pageSize)));
  }

  /**
   * 查询模板详情与食材用量。
   * @param request 当前 HTTP 请求
   * @param templateId 模板 ID
   * @return 模板详情
   */
  @GetMapping("/dish-templates/{templateId}")
  @Operation(summary = "查询模板菜品详情", description = "返回简介、授权图片、标签和食材，不包含制作步骤。")
  public ApiResponse<DishTemplateDetailView> detail(HttpServletRequest request,
      @Parameter(description = "平台模板ID", required = true) @PathVariable Long templateId) {
    return ApiResponse.ok(templateService.detail(requireMerchant(request), templateId));
  }

  /**
   * 将所选平台模板批量导入当前商户菜品库。
   * @param request 当前 HTTP 请求
   * @param body 模板 ID 列表
   * @return 导入和跳过统计
   */
  @PostMapping("/dish-templates/import")
  @Operation(summary = "选择性批量导入模板", description = "模板复制为当前商户独立菜品；已导入模板自动跳过。")
  public ApiResponse<DishTemplateImportResultView> importTemplates(HttpServletRequest request,
      @Valid @RequestBody DishTemplateImportRequest body) {
    return ApiResponse.ok(templateService.importTemplates(requireMerchant(request), body.templateIds()));
  }

  /**
   * 将全部启用平台模板导入当前商户菜品库，已导入模板自动跳过。
   * @param request 当前 HTTP 请求
   * @return 导入和跳过统计
   */
  @PostMapping("/dish-templates/import-all")
  @Operation(summary = "导入全部启用模板", description = "一次导入全部启用模板；当前商户已导入的模板自动跳过。")
  public ApiResponse<DishTemplateImportResultView> importAllTemplates(HttpServletRequest request) {
    return ApiResponse.ok(templateService.importAllTemplates(requireMerchant(request)));
  }

  private CurrentUserContext requireMerchant(HttpServletRequest request) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasMerchantBackendAccess() || user.merchantId() == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无商户后台访问权限");
    }
    return user;
  }
}
