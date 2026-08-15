package com.familykitchen.dish.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.dto.DishTemplateQuery;
import com.familykitchen.dish.model.vo.DishTemplateCategoryView;
import com.familykitchen.dish.model.vo.DishTemplateDetailView;
import com.familykitchen.dish.model.vo.DishTemplateImportResultView;
import com.familykitchen.dish.model.vo.DishTemplatePageView;
import java.util.List;

/** 平台菜品模板查询与商户选择性导入服务。 */
public interface DishTemplateService {
  /**
   * 查询平台启用的模板分类。
   * @return 按排序值排列的模板分类
   */
  List<DishTemplateCategoryView> categories();
  /**
   * 按当前商户的导入状态和筛选条件分页查询模板。
   * @param user 当前商户用户上下文
   * @param query 筛选和分页参数
   * @return 模板分页结果
   */
  DishTemplatePageView page(CurrentUserContext user, DishTemplateQuery query);
  /**
   * 查询模板详情和食材用量，不返回制作步骤。
   * @param user 当前商户用户上下文
   * @param templateId 模板 ID
   * @return 模板详情
   */
  DishTemplateDetailView detail(CurrentUserContext user, Long templateId);
  /**
   * 将所选模板复制为当前商户独立可编辑的菜品。
   * @param user 当前商户用户上下文
   * @param templateIds 所选模板 ID
   * @return 实际导入和跳过的模板统计
   */
  DishTemplateImportResultView importTemplates(CurrentUserContext user, List<Long> templateIds);
  /**
   * 将全部启用模板复制为当前商户独立可编辑的菜品。
   * @param user 当前商户用户上下文
   * @return 实际导入和幂等跳过的模板统计
   */
  DishTemplateImportResultView importAllTemplates(CurrentUserContext user);
}
