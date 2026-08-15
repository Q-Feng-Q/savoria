package com.familykitchen.family.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.model.dto.UpdateFamilyDeliveryPolicyRequest;
import com.familykitchen.family.model.dto.UpdateMerchantFamilyProfileRequest;
import com.familykitchen.family.model.vo.MerchantFamilyDetailView;
import com.familykitchen.family.model.vo.MerchantFamilySummaryView;
import java.util.List;

/**
 * 商户端家庭资料维护服务。
 *
 * <p>用于商户后台查看已绑定家庭、维护家庭基础资料和配送策略。</p>
 */
public interface MerchantFamilyApplicationService {

  /**
   * 查询当前商户绑定的家庭列表。
   *
   * @param user 当前登录用户上下文
   * @return 家庭摘要列表
   */
  List<MerchantFamilySummaryView> listFamilies(CurrentUserContext user);

  /**
   * 查询指定家庭的商户端详情。
   *
   * @param user 当前登录用户上下文
   * @param familyId 家庭 ID
   * @return 家庭详情，包含联系人、配送策略等资料
   */
  MerchantFamilyDetailView detail(CurrentUserContext user, Long familyId);

  /**
   * 修改指定家庭基础资料。
   *
   * @param user 当前登录用户上下文
   * @param familyId 家庭 ID
   * @param request 家庭名称、备注、联系人等修改内容
   */
  void updateProfile(CurrentUserContext user, Long familyId, UpdateMerchantFamilyProfileRequest request);

  /**
   * 修改指定家庭配送策略。
   *
   * @param user 当前登录用户上下文
   * @param familyId 家庭 ID
   * @param request 配送开关、默认配送费、免配送费规则
   */
  void updateDeliveryPolicy(CurrentUserContext user, Long familyId, UpdateFamilyDeliveryPolicyRequest request);
}
