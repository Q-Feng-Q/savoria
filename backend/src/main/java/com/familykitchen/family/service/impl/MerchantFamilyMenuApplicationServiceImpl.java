package com.familykitchen.family.service.impl;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.CopyFamilyMenuRequest;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.service.MerchantFamilyMenuApplicationService;
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

  /**
   * 创建商户家庭菜单实例。
   *
   * @param familyMapper 家庭Mapper
   */
  public MerchantFamilyMenuApplicationServiceImpl(FamilyMapper familyMapper) {
    this.familyMapper = familyMapper;
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
    requireFamily(user.merchantId(), familyId);
    for (SaveFamilyMenuRequest.MenuItem item : request.items()) requireDish(user.merchantId(), item.dishId());
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
    requireFamily(user.merchantId(), familyId);
    requireFamily(user.merchantId(), request.sourceFamilyId());
    familyMapper.deleteFamilyMenu(familyId);
    familyMapper.copyFamilyMenu(familyId, request.sourceFamilyId());
  }

  private void requireFamily(Long merchantId, Long familyId) {
    if (familyMapper.countFamilyOwnership(merchantId, familyId) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在或不属于当前商户");
    }
  }

  private void requireDish(Long merchantId, Long dishId) {
    if (familyMapper.countDishOwnership(merchantId, dishId) == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_INVALID, "菜单菜品不存在或不属于当前商户");
    }
  }
}
