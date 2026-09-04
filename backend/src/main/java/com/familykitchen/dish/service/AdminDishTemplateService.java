package com.familykitchen.dish.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.dto.AdminDishTemplateQuery;
import com.familykitchen.dish.model.dto.AdminDishTemplateUpdateRequest;
import com.familykitchen.dish.model.dto.DishTemplateImagePromotionRequest;
import com.familykitchen.dish.model.dto.DishTemplateImageRejectionRequest;
import com.familykitchen.dish.model.entity.DishTemplateSourceRecordEntity;
import com.familykitchen.dish.model.vo.AdminDishTemplateDetailView;
import com.familykitchen.dish.model.vo.AdminDishTemplatePageView;
import com.familykitchen.dish.model.vo.DishTemplateImageAssetStatusView;
import com.familykitchen.dish.model.vo.DishTemplateMutationView;
import java.util.List;

/** 平台菜谱模板维护和受控图片审核服务。 */
public interface AdminDishTemplateService {
  /**
   * 分页查询平台全部模板。
   * @param user 当前用户
   * @param query 查询条件
   * @return 模板分页结果
   */
  AdminDishTemplatePageView page(CurrentUserContext user, AdminDishTemplateQuery query);
  /**
   * 查询平台模板完整详情。
   * @param user 当前用户
   * @param templateId 模板ID
   * @return 模板完整详情
   */
  AdminDishTemplateDetailView detail(CurrentUserContext user, Long templateId);
  /**
   * 查询模板来源记录。
   * @param user 当前用户
   * @param templateId 模板ID
   * @return 模板来源记录
   */
  List<DishTemplateSourceRecordEntity> sourceRecords(CurrentUserContext user, Long templateId);
  /**
   * 覆盖模板可编辑字段并重新计算派生状态。
   * @param user 当前用户
   * @param templateId 模板ID
   * @param request 完整编辑快照
   * @return 变更结果
   */
  DishTemplateMutationView update(CurrentUserContext user, Long templateId,
      AdminDishTemplateUpdateRequest request);
  /**
   * 读取允许预览的内部图片。
   * @param user 当前用户
   * @param assetId 资源ID
   * @return 受控预览资源
   */
  DishTemplateImageStorageService.PreviewResource preview(CurrentUserContext user, Long assetId);
  /**
   * 审核并发布模板图片。
   * @param user 当前用户
   * @param templateId 模板ID
   * @param request 授权发布声明
   * @return 变更结果
   */
  DishTemplateMutationView promoteImage(CurrentUserContext user, Long templateId,
      DishTemplateImagePromotionRequest request);
  /**
   * 永久驳回待审核图片。
   * @param user 当前用户
   * @param templateId 模板ID
   * @param assetId 资源ID
   * @param request 驳回原因
   * @return 图片审核状态
   */
  DishTemplateImageAssetStatusView rejectImage(CurrentUserContext user, Long templateId, Long assetId,
      DishTemplateImageRejectionRequest request);
}
