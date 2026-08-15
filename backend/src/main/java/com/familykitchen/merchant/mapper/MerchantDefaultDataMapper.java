package com.familykitchen.merchant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 从系统基础商户的实际业务目录复制数据到指定商户。 */
@Mapper
public interface MerchantDefaultDataMapper {

  /**
   * 补齐商户缺少的默认菜品分类。
   *
   * @param merchantId 商户标识
   * @return 新增分类数量
   */
  @Insert("""
      insert ignore into dish_categories (merchant_id, name, sort_order, enabled)
      select #{merchantId}, name, sort_order, 1
      from dish_categories
      where merchant_id = 1
      order by sort_order, id
      """)
  int copyDefaultCategories(@Param("merchantId") Long merchantId);

  /**
   * 补齐商户缺少的默认食材。
   *
   * @param merchantId 商户标识
   * @return 新增食材数量
   */
  @Insert("""
      insert ignore into merchant_ingredients (merchant_id, name, category, unit)
      select #{merchantId}, name, category, unit
      from merchant_ingredients
      where merchant_id = 1
      order by category, id
      """)
  int copyDefaultIngredients(@Param("merchantId") Long merchantId);
}
