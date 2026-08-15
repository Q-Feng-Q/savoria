package com.familykitchen.family.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.model.dto.CopyFamilyMenuRequest;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import java.util.List;

/**
 * 商户端家庭菜单配置服务。
 *
 * <p>用于按家庭配置可点菜品、家庭专属价格、启用状态和菜单排序。</p>
 */
public interface MerchantFamilyMenuApplicationService {

  /**
   * 查询指定家庭的菜单配置。
   *
   * @param user 当前登录用户上下文
   * @param familyId 家庭 ID
   * @return 家庭菜单项列表
   */
  List<FamilyMenuItemView> menu(CurrentUserContext user, Long familyId);

  /**
   * 保存指定家庭的菜单配置。
   *
   * @param user 当前登录用户上下文
   * @param familyId 家庭 ID
   * @param request 菜品启用状态、排序和家庭价配置
   */
  void saveMenu(CurrentUserContext user, Long familyId, SaveFamilyMenuRequest request);

  /**
   * 从另一个家庭复制菜单配置到当前家庭。
   *
   * @param user 当前登录用户上下文
   * @param familyId 目标家庭 ID
   * @param request 来源家庭 ID
   */
  void copyMenu(CurrentUserContext user, Long familyId, CopyFamilyMenuRequest request);
}
