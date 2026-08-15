package com.familykitchen.dish.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.DishCategoryRequest;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.dto.DishStatusRequest;
import com.familykitchen.dish.model.dto.DishMutationResult;
import com.familykitchen.dish.model.entity.DishCategoryEntity;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.vo.DishCategoryView;
import com.familykitchen.dish.model.vo.DishDetailView;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.system.service.SystemSettingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户侧菜品应用服务实现。
 *
 * <p>负责商户菜品、菜品分类以及制作步骤的维护与查询，统一把当前登录用户中的商户 ID
 * 传递到存储层，防止跨商户读写。</p>
 */
@Service
public class DishApplicationServiceImpl implements DishApplicationService {

  private final DishMapper dishMapper;
  private final SystemSettingService systemSettingService;
  private final DishReviewService dishReviewService;

  /**
   * 创建菜品实例。
   *
   * @param dishMapper 菜品Mapper
   * @param systemSettingService system配置Service
   * @param dishReviewService 菜品审核Service
   */
  public DishApplicationServiceImpl(DishMapper dishMapper,SystemSettingService systemSettingService,
                                    DishReviewService dishReviewService) {
    this.dishMapper = dishMapper;
    this.systemSettingService=systemSettingService;
    this.dishReviewService=dishReviewService;
  }

  /**
   * 处理菜品。
   *
   * @param user 用户
   * @return 处理的结果
   */
  @Override
  public List<DishView> dishes(CurrentUserContext user) {
    return dishMapper.selectDishes(user.merchantId()).stream().map(DishApplicationServiceImpl::toDishView).toList();
  }

  /**
   * 创建菜品。
   *
   * @param user 用户
   * @param request 请求参数
   * @return 创建菜品的结果
   */
  @Override
  @Transactional
  public DishView createDish(CurrentUserContext user, DishRequest request) {
    requireCategory(user.merchantId(), request.categoryId());
    if(systemSettingService.dishReviewEnabled()){
      dishReviewService.submit(user.userId(),user.merchantId(),null,request);
      return new DishView(null,request.categoryId(),request.name(),request.description(),request.imageUrl(),
          money(request.basePrice()),"PENDING_REVIEW");
    }
    // 菜品主信息、食材配方和制作步骤放在同一事务中创建，避免出现半成品数据。
    DishEntity entity = toEntity(user.merchantId(), null, request);
    dishMapper.insertDish(entity);
    replaceIngredients(entity.getId(), request.ingredients());
    replaceCookingSteps(entity.getId(), request.cookingSteps());
    return toDishView(entity);
  }

  /**
   * 更新菜品。
   *
   * @param user 用户
   * @param dishId 菜品标识
   * @param request 请求参数
   */
  /** Updates a complete dish and reports whether it was applied or submitted.
   * @param user current merchant user
   * @param dishId dish identifier
   * @param request complete dish mutation
   * @return truthful mutation outcome
   */
  @Override
  @Transactional
  public DishMutationResult updateDish(CurrentUserContext user, Long dishId, DishRequest request) {
    requireDish(user.merchantId(), dishId);
    requireCategory(user.merchantId(), request.categoryId());
    if(systemSettingService.dishReviewEnabled()){
      dishReviewService.submit(user.userId(),user.merchantId(),dishId,request);
      return new DishMutationResult(DishMutationResult.Outcome.PENDING_REVIEW);
    }
    DishEntity entity = toEntity(user.merchantId(), dishId, request);
    dishMapper.updateDish(entity);
    replaceIngredients(dishId, request.ingredients());
    replaceCookingSteps(dishId, request.cookingSteps());
    return new DishMutationResult(DishMutationResult.Outcome.APPLIED);
  }

  /** Updates status directly or submits a full review snapshot when review is enabled.
   * @param user current merchant user
   * @param dishId dish identifier
   * @param request requested status
   * @return truthful mutation outcome
   */
  @Override
  @Transactional
  public DishMutationResult updateDishStatus(CurrentUserContext user, Long dishId, DishStatusRequest request) {
    DishEntity current = requireDish(user.merchantId(), dishId);
    String status = request.status().toLowerCase();
    if (systemSettingService.dishReviewEnabled()) {
      List<DishRequest.IngredientRequest> ingredients = dishMapper.selectDishIngredients(dishId).stream()
          .map(item -> new DishRequest.IngredientRequest(item.getIngredientName(), item.getQuantity(), item.getUnit(), item.getCalcType()))
          .toList();
      List<DishRequest.CookingStepRequest> steps = dishMapper.selectCookingSteps(dishId).stream()
          .map(item -> new DishRequest.CookingStepRequest(item.getStepNo(), item.getTitle(), item.getContent()))
          .toList();
      DishRequest snapshot = new DishRequest(current.getName(), current.getCategoryId(), current.getDescription(),
          current.getImageUrl(), current.getBasePrice(), ingredients, steps, status);
      dishReviewService.submit(user.userId(), user.merchantId(), dishId, snapshot);
      return new DishMutationResult(DishMutationResult.Outcome.PENDING_REVIEW);
    }
    if (dishMapper.updateDishStatus(user.merchantId(), dishId, status) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
    }
    return new DishMutationResult(DishMutationResult.Outcome.APPLIED);
  }

  /**
   * 更新CookingSteps。
   *
   * @param user 用户
   * @param dishId 菜品标识
   * @param steps steps
   */
  @Override
  @Transactional
  public void updateCookingSteps(CurrentUserContext user, Long dishId, List<DishRequest.CookingStepRequest> steps) {
    DishEntity current=requireDish(user.merchantId(), dishId);
    if(systemSettingService.dishReviewEnabled()){
      List<DishRequest.IngredientRequest> ingredients=dishMapper.selectDishIngredients(dishId).stream()
          .map(i->new DishRequest.IngredientRequest(i.getIngredientName(),i.getQuantity(),i.getUnit(),i.getCalcType())).toList();
      DishRequest snapshot=new DishRequest(current.getName(),current.getCategoryId(),current.getDescription(),
          current.getImageUrl(),current.getBasePrice(),ingredients,steps,current.getStatus());
      dishReviewService.submit(user.userId(),user.merchantId(),dishId,snapshot);
      return;
    }
    replaceCookingSteps(dishId, steps);
  }

  /**
   * 处理菜品。
   *
   * @param user 用户
   * @return 处理的结果
   */
  @Override
  public List<DishCategoryView> categories(CurrentUserContext user) {
    return dishMapper.selectCategories(user.merchantId()).stream().map(DishApplicationServiceImpl::toCategoryView).toList();
  }

  /**
   * 创建Category。
   *
   * @param user 用户
   * @param request 请求参数
   * @return 创建Category的结果
   */
  @Override
  @Transactional
  public DishCategoryView createCategory(CurrentUserContext user, DishCategoryRequest request) {
    DishCategoryEntity entity = new DishCategoryEntity();
    entity.setMerchantId(user.merchantId());
    entity.setName(request.name());
    entity.setSortOrder(request.sortOrder());
    entity.setEnabled(request.enabled());
    dishMapper.insertCategory(entity);
    return toCategoryView(entity);
  }

  /**
   * 更新Category。
   *
   * @param user 用户
   * @param categoryId category标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void updateCategory(CurrentUserContext user, Long categoryId, DishCategoryRequest request) {
    DishCategoryEntity entity = new DishCategoryEntity();
    entity.setId(categoryId);
    entity.setMerchantId(user.merchantId());
    entity.setName(request.name());
    entity.setSortOrder(request.sortOrder());
    entity.setEnabled(request.enabled());
    if (dishMapper.updateCategory(entity) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到菜品分类");
    }
  }

  /**
   * 删除Category。
   *
   * @param user 用户
   * @param categoryId category标识
   */
  @Override
  @Transactional
  public void deleteCategory(CurrentUserContext user, Long categoryId) {
    if (dishMapper.countDishesInCategory(user.merchantId(), categoryId) > 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "分类下存在菜品，不能删除");
    }
    if (dishMapper.deleteCategory(user.merchantId(), categoryId) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到菜品分类");
    }
  }

  /**
   * 处理菜品。
   *
   * @param user 用户
   * @param dishId 菜品标识
   * @return 处理的结果
   */
  @Override
  public DishDetailView detail(CurrentUserContext user, Long dishId) {
    DishEntity dish = requireDish(user.merchantId(), dishId);
    return toDetailView(dish);
  }

  /**
   * 校验菜品属于当前商户，并返回菜品实体。
   */
  private DishEntity requireDish(Long merchantId, Long dishId) {
    DishEntity dish = dishMapper.selectDish(merchantId, dishId);
    if (dish == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到菜品");
    }
    return dish;
  }

  /** 校验分类存在且归属于当前商户，防止无外键模式下产生悬空或跨商户引用。 */
  private void requireCategory(Long merchantId, Long categoryId) {
    if (dishMapper.countCategoryOwnership(merchantId, categoryId) == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "菜品分类不存在或不属于当前商户");
    }
  }

  /**
   * 用最新传入的食材列表整体替换菜品配方，避免局部更新后遗留旧数据。
   */
  private void replaceIngredients(Long dishId, List<DishRequest.IngredientRequest> ingredients) {
    dishMapper.deleteDishIngredients(dishId);
    if (ingredients == null) {
      return;
    }
    for (DishRequest.IngredientRequest item : ingredients) {
      DishIngredientEntity entity = new DishIngredientEntity();
      entity.setDishId(dishId);
      entity.setIngredientName(item.ingredientName());
      entity.setQuantity(money(item.quantity()));
      entity.setUnit(item.unit());
      entity.setCalcType(item.calcType());
      dishMapper.insertDishIngredient(entity);
    }
  }

  /**
   * 用最新制作步骤整体替换原步骤，保证顺序和内容与前端编辑结果一致。
   */
  private void replaceCookingSteps(Long dishId, List<DishRequest.CookingStepRequest> steps) {
    dishMapper.deleteCookingSteps(dishId);
    if (steps == null) {
      return;
    }
    for (DishRequest.CookingStepRequest item : steps) {
      DishCookingStepEntity entity = new DishCookingStepEntity();
      entity.setDishId(dishId);
      entity.setStepNo(item.stepNo());
      entity.setTitle(item.title());
      entity.setContent(item.content());
      dishMapper.insertCookingStep(entity);
    }
  }

  /**
   * 将菜品请求转换为实体，并统一处理金额精度与默认状态。
   */
  private static DishEntity toEntity(Long merchantId, Long dishId, DishRequest request) {
    DishEntity entity = new DishEntity();
    entity.setId(dishId);
    entity.setMerchantId(merchantId);
    entity.setCategoryId(request.categoryId());
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setImageUrl(request.imageUrl());
    entity.setBasePrice(money(request.basePrice()));
    entity.setStatus(request.status() == null || request.status().isBlank() ? "active" : request.status());
    return entity;
  }

  /**
   * 组装菜品详情视图，补齐食材配方和制作步骤。
   */
  private DishDetailView toDetailView(DishEntity dish) {
    return new DishDetailView(
        dish.getId(),
        dish.getCategoryId(),
        dish.getName(),
        dish.getDescription(),
        dish.getImageUrl(),
        money(dish.getBasePrice()),
        dish.getStatus(),
        dishMapper.selectDishIngredients(dish.getId()).stream()
            .map(item -> new DishDetailView.IngredientView(item.getIngredientName(), money(item.getQuantity()), item.getUnit(), item.getCalcType()))
            .toList(),
        dishMapper.selectCookingSteps(dish.getId()).stream()
            .map(item -> new DishDetailView.CookingStepView(item.getStepNo(), item.getTitle(), item.getContent()))
            .toList()
    );
  }

  /**
   * 组装菜品列表视图。
   */
  private static DishView toDishView(DishEntity entity) {
    return new DishView(
        entity.getId(),
        entity.getCategoryId(),
        entity.getName(),
        entity.getDescription(),
        entity.getImageUrl(),
        money(entity.getBasePrice()),
        entity.getStatus()
    );
  }

  /**
   * 组装菜品分类视图，并对空排序值做兜底处理。
   */
  private static DishCategoryView toCategoryView(DishCategoryEntity entity) {
    return new DishCategoryView(
        entity.getId(),
        entity.getName(),
        entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
        Boolean.TRUE.equals(entity.getEnabled())
    );
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
