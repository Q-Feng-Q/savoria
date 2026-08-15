package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证平台菜品模板市场的表结构和初始化数据完整性。 */
class DishTemplateMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V4__init_dish_template_market.sql");

  @Test
  void migrationContainsIndependentTemplateTablesWithoutPhysicalForeignKeys() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    String lowerSql = sql.toLowerCase();
    assertTrue(sql.contains("CREATE TABLE dish_template_categories"));
    assertTrue(sql.contains("CREATE TABLE dish_templates"));
    assertTrue(sql.contains("CREATE TABLE dish_template_ingredients"));
    assertFalse(lowerSql.contains("foreign key"));
    assertFalse(lowerSql.contains(" references "));
  }

  @Test
  void migrationSeedsExactlyNineCategoriesAndOneHundredNinetyEightTemplates() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertEquals(9, countLines(sql, "INSERT INTO dish_template_categories "));
    assertEquals(198, countLines(sql, "INSERT INTO dish_templates "));
    assertTrue(countLines(sql, "INSERT INTO dish_template_ingredients ") >= 198);
    assertEquals(198, countLines(sql, "-- TEMPLATE "));
  }

  @Test
  void everyTemplateCarriesImageAttributionAndNoCookingSteps() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    for (String line : sql.lines().map(String::trim).toList()) {
      if (!line.startsWith("INSERT INTO dish_templates ")) continue;
      assertTrue(line.contains("image_source_url,image_author,image_license"));
      assertTrue(line.contains("/images/dish-templates/"));
    }
    assertFalse(sql.contains("INSERT INTO dish_cooking_steps"));
  }

  @Test
  void merchantDishTableDeclaresTemplateOriginAndDeduplicationIndex() throws Exception {
    String sql = Files.readString(
        Path.of("src/main/resources/db/migration/V2__init_menu_order_wallet_and_purchase.sql"),
        StandardCharsets.UTF_8);
    assertTrue(sql.contains("source_template_id bigint NULL COMMENT '来源平台模板菜品ID'"));
    assertTrue(sql.contains("UNIQUE KEY uk_dishes_merchant_template (merchant_id,source_template_id)"));
  }

  private static long countLines(String sql, String prefix) {
    return sql.lines().map(String::trim).filter(line -> line.startsWith(prefix)).count();
  }
}
