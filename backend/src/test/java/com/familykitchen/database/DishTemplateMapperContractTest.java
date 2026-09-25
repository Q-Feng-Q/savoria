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
        "selectEligibleTemplateForUpdate",
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
    assertTrue(xml.contains("c.enabled=1"));
  }

  @Test
  void merchantMarketShowsAllEnabledDishesWhileImportsRemainStrictlyEligible() throws Exception {
    String xml = Files.readString(
        Path.of("src/main/resources/mapper/dish/DishTemplateMapper.xml"), StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ");
    String predicate = "t.template_type='DISH' and t.data_status='READY' and "
        + "t.procurement_ready=1 and t.reference_price is not null and t.enabled=1";
    String visiblePredicate = "t.template_type='DISH' and t.enabled=1";
    assertTrue(xml.contains("<sql id=\"MerchantEligibleTemplateFilter\"> " + predicate));
    assertTrue(xml.contains("<sql id=\"MerchantVisibleTemplateFilter\"> " + visiblePredicate));
    String listQuery = xml.substring(xml.indexOf("id=\"selectTemplates\""),
        xml.indexOf("</select>", xml.indexOf("id=\"selectTemplates\"")));
    assertTrue(listQuery.contains("TemplateFilters"));
    String listFilters = xml.substring(xml.indexOf("id=\"TemplateFilters\""),
        xml.indexOf("</sql>", xml.indexOf("id=\"TemplateFilters\"")));
    assertTrue(listFilters.contains("MerchantVisibleTemplateFilter"));
    assertTrue(!listFilters.contains("MerchantEligibleTemplateFilter"));
    String importQuery = xml.substring(xml.indexOf("id=\"selectTemplatesByIds\""),
        xml.indexOf("</select>", xml.indexOf("id=\"selectTemplatesByIds\"")));
    assertTrue(importQuery.contains("MerchantEligibleTemplateFilter"));
    assertTrue(xml.contains("id=\"selectAdminTemplates\""));
    String adminQuery = xml.substring(xml.indexOf("id=\"selectAdminTemplates\""),
        xml.indexOf("</select>", xml.indexOf("id=\"selectAdminTemplates\"")));
    assertTrue(!adminQuery.contains("MerchantEligibleTemplateFilter"));
  }

  @Test
  void mapperMapsNullableFieldsAndCompleteRecipeRelations() throws Exception {
    String xml = Files.readString(
        Path.of("src/main/resources/mapper/dish/DishTemplateMapper.xml"), StandardCharsets.UTF_8);
    for (String resultMap : new String[] {
        "TemplateMap", "TemplateIngredientMap", "TemplateCookingStepMap", "TemplateSourceRecordMap",
        "TemplateNameAliasMap", "TemplateImageAssetMap"
    }) {
      assertTrue(xml.contains("id=\"" + resultMap + "\""), "缺少结果映射: " + resultMap);
    }
    for (String statement : new String[] {
        "selectTemplateCookingSteps", "selectTemplateSourceRecords", "selectTemplateNameAliases",
        "selectTemplateImageAssets", "deleteTemplateCookingSteps", "insertTemplateCookingStep"
    }) {
      assertTrue(xml.contains("id=\"" + statement + "\""), "缺少 Mapper SQL: " + statement);
    }
    assertTrue(xml.contains("order by step_no,id"));
    assertTrue(xml.contains("quantity_status"));
    assertTrue(xml.contains("component_occurrence_key"));
    assertTrue(xml.contains("rejection_reason"));
  }
}
