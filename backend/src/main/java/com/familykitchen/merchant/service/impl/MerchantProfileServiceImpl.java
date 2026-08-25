package com.familykitchen.merchant.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.merchant.mapper.MerchantProfileMapper;
import com.familykitchen.merchant.model.dto.UpdateMerchantProfileRequest;
import com.familykitchen.merchant.model.vo.MerchantProfileView;
import com.familykitchen.merchant.service.MerchantProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商户负责人自助资料服务实现。 */
@Service
public class MerchantProfileServiceImpl implements MerchantProfileService {
  private final MerchantProfileMapper merchantProfileMapper;

  /**
   * 创建商户资料服务。
   * @param merchantProfileMapper 商户资料数据访问接口
   */
  public MerchantProfileServiceImpl(MerchantProfileMapper merchantProfileMapper) {
    this.merchantProfileMapper = merchantProfileMapper;
  }

  /**
   * 查询当前负责人的商户资料，并以数据库关系作为最终权限依据。
   * @param user 当前用户上下文
   * @return 商户业务资料
   */
  @Override
  public MerchantProfileView profile(CurrentUserContext user) {
    requireMerchantContext(user);
    MerchantProfileView profile = merchantProfileMapper.selectProfile(user.userId(), user.merchantId());
    if (profile == null) {
      throw denied();
    }
    return profile;
  }

  /**
   * 规范化白名单字段并在实时关系校验下更新商户资料。
   * @param user 当前用户上下文
   * @param request 商户资料更新请求
   * @return 更新后的商户业务资料
   */
  @Override
  @Transactional
  public MerchantProfileView updateProfile(CurrentUserContext user,
      UpdateMerchantProfileRequest request) {
    requireMerchantContext(user);
    String name = request == null ? null : normalizeRequired(request.name());
    if (name == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "商户名称不能为空");
    }
    String contactName = normalizeOptional(request.contactName());
    String contactPhone = normalizeOptional(request.contactPhone());
    int updated = merchantProfileMapper.updateProfile(
        user.userId(), user.merchantId(), name, contactName, contactPhone);
    if (updated != 1) {
      throw denied();
    }
    return new MerchantProfileView(name, contactName, contactPhone);
  }

  private static void requireMerchantContext(CurrentUserContext user) {
    if (user == null || user.userId() == null || user.merchantId() == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号没有有效商户身份");
    }
  }

  private static String normalizeRequired(String value) {
    String normalized = normalizeOptional(value);
    return normalized == null || normalized.isEmpty() ? null : normalized;
  }

  private static String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static BusinessException denied() {
    return new BusinessException(ErrorCode.FORBIDDEN, "商户负责人权限已失效，请重新选择身份");
  }
}
