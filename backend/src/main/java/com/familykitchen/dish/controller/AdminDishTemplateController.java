package com.familykitchen.dish.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.AdminDishTemplateQuery;
import com.familykitchen.dish.model.dto.AdminDishTemplateUpdateRequest;
import com.familykitchen.dish.model.dto.DishTemplateImagePromotionRequest;
import com.familykitchen.dish.model.dto.DishTemplateImageRejectionRequest;
import com.familykitchen.dish.model.entity.DishTemplateSourceRecordEntity;
import com.familykitchen.dish.model.vo.AdminDishTemplateDetailView;
import com.familykitchen.dish.model.vo.AdminDishTemplatePageView;
import com.familykitchen.dish.model.vo.DishTemplateImageAssetStatusView;
import com.familykitchen.dish.model.vo.DishTemplateMutationView;
import com.familykitchen.dish.service.AdminDishTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 平台管理员维护完整菜谱模板与审核内部图片的接口。 */
@RestController
@RequestMapping("/admin")
@Tag(name = "平台端-菜谱模板", description = "管理全部成品菜、配料组件、制作步骤和图片授权")
public class AdminDishTemplateController {
  private final CurrentUserProvider currentUserProvider;
  private final AdminDishTemplateService service;

  /**
   * 创建平台模板管理控制器。
   * @param currentUserProvider 当前登录用户解析器
   * @param service 平台模板管理服务
   */
  public AdminDishTemplateController(CurrentUserProvider currentUserProvider,
      AdminDishTemplateService service) {
    this.currentUserProvider = currentUserProvider;
    this.service = service;
  }

  /**
   * 分页查询全部模板和组件。
   * @param request 当前HTTP请求
   * @param page 页码
   * @param pageSize 每页数量
   * @param keyword 菜名关键词
   * @param sourceType 来源类型
   * @param templateType 模板类型
   * @param dataStatus 数据完整状态
   * @param sourceCategory 来源分类
   * @param missingImage 是否缺少公开图片
   * @param missingSteps 是否缺少制作步骤
   * @return 平台模板分页结果
   */
  @GetMapping("/dish-templates")
  @Operation(summary = "分页查询平台模板", description = "支持来源、类型、完整状态、缺图和缺步骤筛选。")
  public ApiResponse<AdminDishTemplatePageView> page(HttpServletRequest request,
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize,
      @RequestParam(required = false) String keyword, @RequestParam(required = false) String sourceType,
      @RequestParam(required = false) String templateType, @RequestParam(required = false) String dataStatus,
      @RequestParam(required = false) String sourceCategory,
      @RequestParam(required = false) Boolean missingImage,
      @RequestParam(required = false) Boolean missingSteps,
      @RequestParam(required = false) String productType) {
    return ApiResponse.ok(service.page(user(request), new AdminDishTemplateQuery(page, pageSize, keyword,
        sourceType, templateType, dataStatus, sourceCategory, missingImage, missingSteps, productType)));
  }

  /**
   * 查询模板管理详情。
   * @param request 当前HTTP请求
   * @param templateId 模板ID
   * @return 平台模板完整详情
   */
  @GetMapping("/dish-templates/{templateId}")
  @Operation(summary = "查询平台模板详情")
  public ApiResponse<AdminDishTemplateDetailView> detail(HttpServletRequest request,
      @Parameter(description = "模板ID") @PathVariable Long templateId) {
    return ApiResponse.ok(service.detail(user(request), templateId));
  }

  /**
   * 返回模板来源记录和内容摘要。
   * @param request 当前HTTP请求
   * @param templateId 模板ID
   * @return 模板来源记录
   */
  @GetMapping("/dish-templates/{templateId}/source-records")
  @Operation(summary = "查询模板来源记录")
  public ApiResponse<List<DishTemplateSourceRecordEntity>> sourceRecords(HttpServletRequest request,
      @PathVariable Long templateId) {
    return ApiResponse.ok(service.sourceRecords(user(request), templateId));
  }

  /**
   * 使用带版本号的完整快照更新模板可编辑字段。
   * @param request 当前HTTP请求
   * @param templateId 模板ID
   * @param body 完整可编辑快照
   * @return 变更后的模板状态
   */
  @PutMapping("/dish-templates/{templateId}")
  @Operation(summary = "更新平台模板", description = "来源、图片授权和派生状态由服务端维护。")
  public ApiResponse<DishTemplateMutationView> update(HttpServletRequest request,
      @PathVariable Long templateId, @Valid @RequestBody AdminDishTemplateUpdateRequest body) {
    return ApiResponse.ok(service.update(user(request), templateId, body));
  }

  /**
   * 通过受控资源ID预览待审核或已发布图片。
   * @param request 当前HTTP请求
   * @param assetId 受控图片资源ID
   * @return 禁止缓存的图片二进制响应
   */
  @GetMapping("/dish-template-assets/{assetId}/preview")
  @Operation(summary = "预览内部模板图片", description = "不接受路径参数，已驳回图片不可访问。")
  public ResponseEntity<Resource> preview(HttpServletRequest request, @PathVariable Long assetId) {
    var value = service.preview(user(request), assetId);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(value.mimeType())).contentLength(value.contentLength())
        .body(value.resource());
  }

  /**
   * 确认图片授权并发布到公共资源目录。
   * @param request 当前HTTP请求
   * @param templateId 模板ID
   * @param body 图片授权声明与期望版本
   * @return 变更后的模板状态
   */
  @PostMapping("/dish-templates/{templateId}/image-promotion")
  @Operation(summary = "发布模板图片", description = "发布成功后模板版本递增。")
  public ApiResponse<DishTemplateMutationView> promoteImage(HttpServletRequest request,
      @PathVariable Long templateId, @Valid @RequestBody DishTemplateImagePromotionRequest body) {
    return ApiResponse.ok(service.promoteImage(user(request), templateId, body));
  }

  /**
   * 永久驳回内部审核图片。
   * @param request 当前HTTP请求
   * @param templateId 模板ID
   * @param assetId 受控图片资源ID
   * @param body 驳回原因
   * @return 图片最终审核状态
   */
  @PostMapping("/dish-templates/{templateId}/image-assets/{assetId}/reject")
  @Operation(summary = "驳回模板图片", description = "只有待审核图片可驳回，驳回后不能预览或发布。")
  public ApiResponse<DishTemplateImageAssetStatusView> rejectImage(HttpServletRequest request,
      @PathVariable Long templateId, @PathVariable Long assetId,
      @Valid @RequestBody DishTemplateImageRejectionRequest body) {
    return ApiResponse.ok(service.rejectImage(user(request), templateId, assetId, body));
  }

  private CurrentUserContext user(HttpServletRequest request) {
    return currentUserProvider.require(request);
  }
}
