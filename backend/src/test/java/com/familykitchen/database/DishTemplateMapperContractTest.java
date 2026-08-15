package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证菜品模板 Mapper 提供查询和导入闭环所需的显式 SQL。 */
class DishTemplateMapperContractTest {

  @Test
  void mapperContainsTenantScopedQueryAndImportStatements() throws Exception {
    String xml = Files.readString(
        Path.of("src/main/resources/mapper/dish/DishTemplateMapper.xml"), StandardCharsets.UTF_8);
    for (String statement : new String[] {
        "selectTemplateCategories", "countTemplates", "selectTemplates", "selectTemplate",
        "selectTemplateIngredients", "selectTemplatesByIds", "selectAllEnabledTemplates",
        "selectTemplateIngredientsByIds",
        "selectImportedTemplateIds", "selectMerchantCategoryByName", "insertMerchantCategoryIgnore",
        "insertImportedDishIgnore", "selectImportedDishId", "insertMerchantIngredientIgnore"
    }) {
      assertTrue(xml.contains("id=\"" + statement + "\""), "缺少 Mapper SQL: " + statement);
    }
    assertTrue(xml.contains("d.merchant_id = #{merchantId}"));
    assertTrue(xml.contains("source_template_id"));
    assertTrue(xml.contains("insert ignore"));
    assertTrue(xml.contains("<select id=\"selectAllEnabledTemplates\""));
    assertTrue(xml.contains("where t.enabled=1"));
    assertTrue(xml.contains("c.enabled=1"));
  }
}
