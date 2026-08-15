package com.familykitchen.merchant;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.merchant.mapper.MerchantDefaultDataMapper;
import com.familykitchen.merchant.service.MerchantDefaultDataInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.apache.ibatis.annotations.Insert;

/** 验证商户默认菜品分类和食材清单的初始化契约。 */
class MerchantDefaultDataInitializerTest {

  @Test
  void copiesCategoriesBeforeIngredientsForMerchant() {
    MerchantDefaultDataMapper mapper = mock(MerchantDefaultDataMapper.class);
    MerchantDefaultDataInitializer initializer = new MerchantDefaultDataInitializer(mapper);

    initializer.initialize(42L);

    InOrder order = inOrder(mapper);
    order.verify(mapper).copyDefaultCategories(42L);
    order.verify(mapper).copyDefaultIngredients(42L);
  }

  @Test
  void copiesCatalogFromTheRealSystemMerchantInsteadOfDefaultTables() throws Exception {
    String categorySql = String.join(" ", MerchantDefaultDataMapper.class
        .getMethod("copyDefaultCategories", Long.class).getAnnotation(Insert.class).value());
    String ingredientSql = String.join(" ", MerchantDefaultDataMapper.class
        .getMethod("copyDefaultIngredients", Long.class).getAnnotation(Insert.class).value());

    assertTrue(categorySql.contains("from dish_categories"));
    assertTrue(categorySql.contains("merchant_id = 1"));
    assertTrue(ingredientSql.contains("from merchant_ingredients"));
    assertTrue(ingredientSql.contains("merchant_id = 1"));
    assertFalse((categorySql + ingredientSql).contains("default_"));
  }
}
