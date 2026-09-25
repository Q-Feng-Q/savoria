package com.familykitchen.family.service.impl;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.CopyFamilyMenuRequest;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.service.MerchantFamilyMenuApplicationService;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.model.entity.DishEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户侧家庭菜单配置应用服务实现。
 *
 * <p>用于查看指定家庭的菜单配置、保存家庭菜单，以及将一个家庭的菜单模板复制到另一个家庭。</p>
 */
@Service
public class MerchantFamilyMenuApplicationServiceImpl implements MerchantFamilyMenuApplicationService {

  private final FamilyMapper familyMapper;
  private final DishApplicationService dishApplicationService;

  /**
   * 创建商户家庭菜单实例。
   *
   * @param familyMapper 家庭Mapper
   * @param dishApplicationService merchant-wide dish service
   */
  public MerchantFamilyMenuApplicationServiceImpl(FamilyMapper familyMapper,
                                                   DishApplicationService dishApplicationService) {
    this.familyMapper = familyMapper;
    this.dishApplicationService = dishApplicationService;
  }

  /**
   * 处理商户家庭菜单。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @return 处理的结果
   */
  @Override
  public List<FamilyMenuItemView> menu(CurrentUserContext user, Long familyId) {
    return familyMapper.selectFamilyMenuItems(user.merchantId(), familyId);
  }

  /**
   * 保存菜单。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void saveMenu(CurrentUserContext user, Long familyId, SaveFamilyMenuRequest request) {
    familyMapper.lockMerchantForMenu(user.merchantId());
    requireFamily(user.merchantId(), familyId);
    lockAndValidateDishes(user.merchantId(), request.items().stream()
        .map(SaveFamilyMenuRequest.MenuItem::dishId).toList());
    familyMapper.deleteFamilyMenu(familyId);
    for (SaveFamilyMenuRequest.MenuItem item : request.items()) {
      familyMapper.insertFamilyMenuItem(
          familyId,
          item.dishId(),
          item.enabled(),
          item.sortOrder(),
          item.familyFinalPrice()
      );
    }
  }

  /**
   * 复制菜单。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void copyMenu(CurrentUserContext user, Long familyId, CopyFamilyMenuRequest request) {
    familyMapper.lockMerchantForMenu(user.merchantId());
    requireFamily(user.merchantId(), familyId);
    requireFamily(user.merchantId(), request.sourceFamilyId());
    lockAndValidateDishes(user.merchantId(), familyMapper.selectFamilyMenuDishIds(request.sourceFamilyId()));
    familyMapper.deleteFamilyMenu(familyId);
    familyMapper.copyFamilyMenu(familyId, request.sourceFamilyId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void setFeaturedDish(CurrentUserContext user, Long familyId, Long dishId) {
    requireFamily(user.merchantId(), familyId);
    dishApplicationService.setFeaturedDish(user, dishId, true);
  }

  private void requireFamily(Long merchantId, Long familyId) {
    if (familyMapper.countFamilyOwnership(merchantId, familyId) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在或不属于当前商户");
    }
  }

  private void lockAndValidateDishes(Long merchantId, List<Long> dishIds) {
    List<Long> sortedIds = dishIds == null ? List.of() : dishIds.stream().distinct().sorted().toList();
    if (sortedIds.isEmpty()) return;
    List<DishEntity> locked = familyMapper.selectOwnedDishesForUpdate(merchantId, sortedIds);
    if (locked == null || locked.size() != sortedIds.size()
        || !locked.stream().map(DishEntity::getId).sorted().toList().equals(sortedIds)
        || locked.stream().anyMatch(item -> "deleted".equalsIgnoreCase(item.getStatus()))) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "菜单菜品不存在、不属于当前商户或已删除");
    }
  }
}
