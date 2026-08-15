package com.familykitchen.dish.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.dto.DishCategoryRequest;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.dto.DishStatusRequest;
import com.familykitchen.dish.model.dto.DishMutationResult;
import com.familykitchen.dish.model.vo.DishCategoryView;
import com.familykitchen.dish.model.vo.DishDetailView;
import com.familykitchen.dish.model.vo.DishView;
import java.util.List;

/**
 * 菜品管理服务。
 *
 * <p>面向商户后台提供菜品、分类、制作流程和菜品详情的业务入口。带
 * {@link CurrentUserContext} 的方法会按当前商户隔离数据；不带用户上下文的方法保留给平台管理、
 * 初始化或测试场景使用。</p>
 */
public interface DishApplicationService {

  /**
   * 查询全部商户菜品列表。
   *
   * @return 菜品列表
   */

  /**
   * 查询当前商户的菜品列表。
   *
   * @param user 当前登录用户上下文
   * @return 当前商户的菜品列表
   */
  List<DishView> dishes(CurrentUserContext user);

  /**
   * 创建平台默认商户范围内的菜品。
   *
   * @param request 菜品基础信息、配方和制作流程
   * @return 创建后的菜品视图
   */

  /**
   * 为当前商户创建菜品。
   *
   * @param user 当前登录用户上下文
   * @param request 菜品基础信息、配方和制作流程
   * @return 创建后的菜品视图
   */
  DishView createDish(CurrentUserContext user, DishRequest request);

  /**
   * 修改指定菜品的基础资料和配方。
   *
   * @param dishId 菜品 ID
   * @param request 菜品修改内容
   * @return truthful mutation outcome
   */

  /**
   * 修改当前商户名下的指定菜品。
   *
   * @param user 当前登录用户上下文
   * @param dishId 菜品 ID
   * @param request 菜品修改内容
   */
  /** Updates a complete dish request.
   * @param user current merchant user
   * @param dishId dish identifier
   * @param request complete dish mutation
   * @return truthful mutation outcome
   */
  DishMutationResult updateDish(CurrentUserContext user, Long dishId, DishRequest request);

  /** Updates or submits for review a dish availability status.
   * @param user current merchant user
   * @param dishId dish identifier
   * @param request requested canonical status
   * @return truthful mutation outcome
   */
  DishMutationResult updateDishStatus(CurrentUserContext user, Long dishId, DishStatusRequest request);

  /**
   * 覆盖指定菜品的制作流程。
   *
   * @param dishId 菜品 ID
   * @param steps 制作步骤列表，按步骤序号保存
   */

  /**
   * 覆盖当前商户指定菜品的制作流程。
   *
   * @param user 当前登录用户上下文
   * @param dishId 菜品 ID
   * @param steps 制作步骤列表，按步骤序号保存
   */
  void updateCookingSteps(CurrentUserContext user, Long dishId, List<DishRequest.CookingStepRequest> steps);

  /**
   * 查询平台默认菜品分类。
   *
   * @return 菜品分类列表
   */

  /**
   * 查询当前商户菜品分类。
   *
   * @param user 当前登录用户上下文
   * @return 菜品分类列表
   */
  List<DishCategoryView> categories(CurrentUserContext user);

  /**
   * 创建平台默认菜品分类。
   *
   * @param request 分类名称、排序和启用状态
   * @return 创建后的分类视图
   */

  /**
   * 为当前商户创建菜品分类。
   *
   * @param user 当前登录用户上下文
   * @param request 分类名称、排序和启用状态
   * @return 创建后的分类视图
   */
  DishCategoryView createCategory(CurrentUserContext user, DishCategoryRequest request);

  /**
   * 修改平台默认分类。
   *
   * @param categoryId 分类 ID
   * @param request 分类修改内容
   */

  /**
   * 修改当前商户名下的分类。
   *
   * @param user 当前登录用户上下文
   * @param categoryId 分类 ID
   * @param request 分类修改内容
   */
  void updateCategory(CurrentUserContext user, Long categoryId, DishCategoryRequest request);

  /**
   * 删除平台默认分类。
   *
   * @param categoryId 分类 ID
   */

  /**
   * 删除当前商户名下的分类。
   *
   * @param user 当前登录用户上下文
   * @param categoryId 分类 ID
   */
  void deleteCategory(CurrentUserContext user, Long categoryId);

  /**
   * 查询菜品详情。
   *
   * @param dishId 菜品 ID
   * @return 菜品详情，包含配方和制作流程
   */

  /**
   * 查询当前商户名下菜品详情。
   *
   * @param user 当前登录用户上下文
   * @param dishId 菜品 ID
   * @return 菜品详情，包含配方和制作流程
   */
  DishDetailView detail(CurrentUserContext user, Long dishId);
}
