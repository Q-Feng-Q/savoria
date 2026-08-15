package com.familykitchen.dish.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.dto.IngredientDictionaryRequest;
import com.familykitchen.dish.model.vo.IngredientDictionaryView;
import java.util.List;

/**
 * 食材字典管理服务。
 *
 * <p>维护商户常用食材主数据，为菜品配方和采购清单聚合提供统一食材来源。</p>
 */
public interface IngredientDictionaryApplicationService {

  /**
   * 查询当前商户的食材字典。
   *
   * @param user 当前登录用户上下文
   * @return 食材字典列表
   */
  List<IngredientDictionaryView> list(CurrentUserContext user);

  /**
   * 新增食材字典项。
   *
   * @param user 当前登录用户上下文
   * @param request 食材名称、分类、单位等资料
   * @return 新增后的食材字典视图
   */
  IngredientDictionaryView create(CurrentUserContext user, IngredientDictionaryRequest request);

  /**
   * 修改食材字典项。
   *
   * @param user 当前登录用户上下文
   * @param ingredientId 食材字典 ID
   * @param request 食材修改内容
   */
  void update(CurrentUserContext user, Long ingredientId, IngredientDictionaryRequest request);

  /**
   * 删除食材字典项。
   *
   * @param user 当前登录用户上下文
   * @param ingredientId 食材字典 ID
   */
  void delete(CurrentUserContext user, Long ingredientId);
}
