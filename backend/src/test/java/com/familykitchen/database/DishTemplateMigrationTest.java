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

  private static final Path SCHEMA = Path.of("src/main/resources/db/migration/V1__init_schema.sql");
  private static final Path CATALOG = Path.of("src/main/resources/db/migration/V3__init_recipe_catalog.sql");

  @Test
  void migrationContainsIndependentTemplateTablesWithoutPhysicalForeignKeys() throws Exception {
    String sql = Files.readString(SCHEMA, StandardCharsets.UTF_8);
    String lowerSql = sql.toLowerCase();
    assertTrue(sql.contains("CREATE TABLE dish_template_categories"));
    assertTrue(sql.contains("CREATE TABLE dish_templates"));
    assertTrue(sql.contains("CREATE TABLE dish_template_ingredients"));
    assertFalse(lowerSql.contains("foreign key"));
    assertFalse(lowerSql.contains(" references "));
  }

  @Test
  void migrationSeedsTheCompleteTemplateCatalog() throws Exception {
    String sql = Files.readString(CATALOG, StandardCharsets.UTF_8);
    assertEquals(9, countLines(sql, "INSERT INTO dish_template_categories "));
    assertEquals(552, countLines(sql, "INSERT INTO dish_templates "));
    assertEquals(2004, countLines(sql, "INSERT INTO dish_template_ingredients "));
  }

  @Test
  void catalogPreservesReviewedImagesAndCookingSteps() throws Exception {
    String sql = Files.readString(CATALOG, StandardCharsets.UTF_8);
    assertEquals(179, countLines(sql, "INSERT INTO dish_template_image_assets "));
    assertEquals(807, countLines(sql, "INSERT INTO dish_template_cooking_steps "));
    assertTrue(sql.contains("'INTERNAL_REVIEW'"));
  }

  @Test
  void merchantDishTableDeclaresTemplateOriginAndDeduplicationIndex() throws Exception {
    String sql = Files.readString(
        SCHEMA,
        StandardCharsets.UTF_8);
    assertTrue(sql.contains("source_template_id bigint DEFAULT NULL COMMENT '来源平台模板菜品ID'"));
    assertTrue(sql.contains("UNIQUE KEY uk_dishes_merchant_template (merchant_id,source_template_id)"));
  }

  private static long countLines(String sql, String prefix) {
    return sql.lines().map(String::trim).filter(line -> line.startsWith(prefix)).count();
  }
}
