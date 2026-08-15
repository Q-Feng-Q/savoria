package com.familykitchen.merchant.service;

import com.familykitchen.merchant.mapper.MerchantDefaultDataMapper;
import org.springframework.stereotype.Service;

/** 从系统基础商户的实际目录为新商户生成可独立维护的分类和食材数据。 */
@Service
public class MerchantDefaultDataInitializer {
  private final MerchantDefaultDataMapper mapper;

  /**
   * 创建商户默认数据初始化器。
   *
   * @param mapper 默认目录数据访问接口
   */
  public MerchantDefaultDataInitializer(MerchantDefaultDataMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * 从商户 1 的实际业务目录补齐目标商户数据；依赖商户维度唯一索引保证重复调用安全。
   *
   * @param merchantId 商户标识
   */
  public void initialize(Long merchantId) {
    mapper.copyDefaultCategories(merchantId);
    mapper.copyDefaultIngredients(merchantId);
  }
}
