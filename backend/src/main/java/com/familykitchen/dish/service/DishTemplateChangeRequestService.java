package com.familykitchen.dish.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.dto.AdminDishTemplateChangeQuery;
import com.familykitchen.dish.model.dto.DishTemplateApproveRequest;
import com.familykitchen.dish.model.dto.DishTemplateChangeSubmitRequest;
import com.familykitchen.dish.model.dto.DishTemplateRejectRequest;
import com.familykitchen.dish.model.dto.ImportedDishTemplateSyncRequest;
import com.familykitchen.dish.model.dto.MerchantDishTemplateChangeQuery;
import com.familykitchen.dish.model.vo.DishTemplateChangeDetailView;
import com.familykitchen.dish.model.vo.DishTemplateChangePageView;
import com.familykitchen.dish.model.vo.DishTemplateChangeSubmitView;

/** 模板菜品修改申请提交、查询、撤回和平台审核服务。 */
public interface DishTemplateChangeRequestService {
  /**
   * 提交当前商户对平台模板菜品的完整修改申请。
   * @param user 当前实时用户上下文
   * @param templateId 平台模板菜品 ID
   * @param request 完整目标快照和提交说明
   * @return 新建申请的 ID、状态和提交时间
   */
  DishTemplateChangeSubmitView submit(CurrentUserContext user, Long templateId,
      DishTemplateChangeSubmitRequest request);
  /**
   * 使用当前商户已导入菜品的实时数据生成来源模板修改申请。
   *
   * <p>菜名、简介、图片、价格和食材来自商户菜品；模板分类、标签、推荐餐次、
   * 图片授权、排序和启用状态保留来源模板当前值，制作步骤随菜品业务快照送审。</p>
   *
   * @param user 当前实时用户上下文
   * @param dishId 当前商户菜品 ID
   * @param request 可选提交说明
   * @return 新建申请的 ID、状态和提交时间
   */
  DishTemplateChangeSubmitView submitFromImportedDish(CurrentUserContext user, Long dishId,
      ImportedDishTemplateSyncRequest request);
  /**
   * 分页查询当前商户的模板菜品修改申请。
   * @param user 当前实时用户上下文
   * @param query 商户查询和分页参数
   * @return 当前商户申请分页结果
   */
  DishTemplateChangePageView merchantPage(CurrentUserContext user, MerchantDishTemplateChangeQuery query);
  /**
   * 查询当前商户单个修改申请和双快照。
   * @param user 当前实时用户上下文
   * @param requestId 修改申请 ID
   * @return 租户隔离后的申请详情
   */
  DishTemplateChangeDetailView merchantDetail(CurrentUserContext user, Long requestId);
  /**
   * 撤回当前商户仍处于待审核状态的申请。
   * @param user 当前实时用户上下文
   * @param requestId 修改申请 ID
   */
  void withdraw(CurrentUserContext user, Long requestId);
  /**
   * 平台管理员分页查询全部模板菜品修改申请。
   * @param user 当前实时用户上下文
   * @param query 平台筛选和分页参数
   * @return 全平台申请分页结果
   */
  DishTemplateChangePageView adminPage(CurrentUserContext user, AdminDishTemplateChangeQuery query);
  /**
   * 平台管理员查询修改申请详情。
   * @param user 当前实时用户上下文
   * @param requestId 修改申请 ID
   * @return 申请详情与提交前后快照
   */
  DishTemplateChangeDetailView adminDetail(CurrentUserContext user, Long requestId);
  /**
   * 审核通过申请并原子覆盖模板主信息和全部食材。
   * @param user 当前平台管理员上下文
   * @param requestId 修改申请 ID
   * @param request 可选审核意见
   */
  void approve(CurrentUserContext user, Long requestId, DishTemplateApproveRequest request);
  /**
   * 驳回待审核申请并向提交商户创建结果通知。
   * @param user 当前平台管理员上下文
   * @param requestId 修改申请 ID
   * @param request 必填驳回原因
   */
  void reject(CurrentUserContext user, Long requestId, DishTemplateRejectRequest request);
}
