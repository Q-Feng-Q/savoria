package com.familykitchen.dish.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.entity.DishEntity;
import org.springframework.stereotype.Component;

/** Shared merchant-to-dish row locking order for dish mutations. */
@Component
public class MerchantDishMutationLock {
  private final DishMapper dishMapper;

  /**
   * Creates the shared locking collaborator.
   * @param dishMapper dish persistence mapper
   */
  public MerchantDishMutationLock(DishMapper dishMapper) {
    this.dishMapper = dishMapper;
  }

  /**
   * Locks the merchant first and then the owned dish.
   * @param merchantId merchant identifier
   * @param dishId dish identifier
   * @return locked owned dish
   */
  public DishEntity lock(Long merchantId, Long dishId) {
    dishMapper.lockMerchant(merchantId);
    DishEntity dish = dishMapper.selectDishForUpdate(merchantId, dishId);
    if (dish == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到菜品");
    }
    return dish;
  }
}
