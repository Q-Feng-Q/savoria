package com.familykitchen.merchant.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.merchant.model.dto.UpdateMerchantProfileRequest;
import com.familykitchen.merchant.model.vo.MerchantProfileView;

/** 商户负责人自助维护商户业务资料的应用服务。 */
public interface MerchantProfileService {

  /**
   * 查询当前负责人所属商户的业务资料。
   * @param user 当前用户上下文
   * @return 商户业务资料
   */
  MerchantProfileView profile(CurrentUserContext user);

  /**
   * 更新当前负责人所属商户的业务资料。
   * @param user 当前用户上下文
   * @param request 更新内容
   * @return 更新后的业务资料
   */
  MerchantProfileView updateProfile(CurrentUserContext user, UpdateMerchantProfileRequest request);
}
