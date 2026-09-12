package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证模板菜品修改审核的行锁、租户过滤和版本更新 SQL。 */
class DishTemplateChangeMapperContractTest {

  @Test
  void requestAndTemplateLocksAreExplicitAndTenantSafe() throws Exception {
    String requests = normalized("src/main/resources/mapper/dish/DishTemplateChangeRequestMapper.xml");
    String templates = normalized("src/main/resources/mapper/dish/DishTemplateMapper.xml");

    assertTrue(requests.contains("id=\"selectForUpdate\"") && requests.contains("where r.id=#{requestId} for update"));
    assertTrue(requests.contains("id=\"selectMerchantForUpdate\"")
        && requests.contains("where r.id=#{requestId} and r.merchant_id=#{merchantId} for update"));
    assertTrue(templates.contains("id=\"selectTemplateForUpdate\"") && templates.contains("for update"));
    assertTrue(templates.contains("id=\"selectCategoryForUpdate\"") && templates.contains("for update"));
  }

  @Test
  void approvedReplacementUpdatesAllEditableFieldsWithVersionGuard() throws Exception {
    String templates = normalized("src/main/resources/mapper/dish/DishTemplateMapper.xml");

    assertTrue(templates.contains("id=\"updateAdminTemplate\""));
    assertFalse(templates.contains("id=\"replaceTemplate\""));
    assertTrue(templates.contains("sort_order=#{template.sortOrder}"));
    assertTrue(templates.contains("version=version+1"));
    assertTrue(templates.contains("where id=#{template.id} and version=#{expectedVersion}"));
    assertTrue(templates.contains("delete from dish_template_ingredients where template_id=#{templateId}"));
    assertTrue(templates.contains("delete from dish_template_cooking_steps where template_id=#{templateId}"));
  }

  private static String normalized(String path) throws Exception {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
  }
}
