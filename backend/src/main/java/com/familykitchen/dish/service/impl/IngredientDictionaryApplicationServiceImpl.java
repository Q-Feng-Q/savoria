package com.familykitchen.dish.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.IngredientDictionaryRequest;
import com.familykitchen.dish.model.entity.IngredientDictionaryEntity;
import com.familykitchen.dish.model.vo.IngredientDictionaryView;
import com.familykitchen.dish.service.IngredientDictionaryApplicationService;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户侧食材字典应用服务实现。
 *
 * <p>维护商户可复用的食材主数据，所有读写都按当前登录用户的商户 ID 隔离，
 * 为菜品配方和采购聚合提供统一食材来源。</p>
 */
@Service
public class IngredientDictionaryApplicationServiceImpl implements IngredientDictionaryApplicationService {

  private final DishMapper dishMapper;

  /**
   * 创建食材Dictionary实例。
   *
   * @param dishMapper 菜品Mapper
   */
  public IngredientDictionaryApplicationServiceImpl(DishMapper dishMapper) {
    this.dishMapper = dishMapper;
  }

  /**
   * 列出食材Dictionary。
   *
   * @param user 用户
   * @return 列出的结果
   */
  @Override
  public List<IngredientDictionaryView> list(CurrentUserContext user) {
    return dishMapper.selectIngredientDictionary(user.merchantId()).stream()
        .map(IngredientDictionaryApplicationServiceImpl::toView)
        .toList();
  }

  /**
   * 创建食材Dictionary。
   *
   * @param user 用户
   * @param request 请求参数
   * @return 创建的结果
   */
  @Override
  @Transactional
  public IngredientDictionaryView create(CurrentUserContext user, IngredientDictionaryRequest request) {
    IngredientDictionaryEntity entity = new IngredientDictionaryEntity();
    entity.setMerchantId(user.merchantId());
    entity.setName(request.name());
    entity.setCategory(request.category());
    entity.setUnit(request.unit());
    dishMapper.insertIngredient(entity);
    return toView(entity);
  }

  /**
   * 更新食材Dictionary。
   *
   * @param user 用户
   * @param ingredientId 食材标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void update(CurrentUserContext user, Long ingredientId, IngredientDictionaryRequest request) {
    IngredientDictionaryEntity current = requireIngredient(user.merchantId(), ingredientId);
    if (!current.getName().equals(request.name())
        && dishMapper.countIngredientReferences(user.merchantId(), current.getName()) > 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "食材已被菜品引用，不能改名");
    }
    current.setName(request.name());
    current.setCategory(request.category());
    current.setUnit(request.unit());
    dishMapper.updateIngredient(current);
  }

  /**
   * 删除食材Dictionary。
   *
   * @param user 用户
   * @param ingredientId 食材标识
   */
  @Override
  @Transactional
  public void delete(CurrentUserContext user, Long ingredientId) {
    IngredientDictionaryEntity current = requireIngredient(user.merchantId(), ingredientId);
    if (dishMapper.countIngredientReferences(user.merchantId(), current.getName()) > 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "食材已被菜品引用，不能删除");
    }
    dishMapper.deleteIngredient(user.merchantId(), ingredientId);
  }

  private IngredientDictionaryEntity requireIngredient(Long merchantId, Long ingredientId) {
    IngredientDictionaryEntity entity = dishMapper.selectIngredient(merchantId, ingredientId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到食材");
    }
    return entity;
  }

  private static IngredientDictionaryView toView(IngredientDictionaryEntity entity) {
    int referencedCount = entity.getReferencedDishCount() == null ? 0 : entity.getReferencedDishCount();
    List<String> names = entity.getReferencedDishNames() == null || entity.getReferencedDishNames().isBlank()
        ? List.of()
        : Arrays.stream(entity.getReferencedDishNames().split(",")).toList();
    return new IngredientDictionaryView(
        entity.getId(),
        entity.getName(),
        entity.getCategory(),
        entity.getUnit(),
        referencedCount,
        names,
        referencedCount == 0
    );
  }
}
