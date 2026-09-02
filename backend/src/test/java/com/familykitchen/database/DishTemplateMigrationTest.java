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
  void migrationContainsCompleteTemplateSchemaWithoutPhysicalForeignKeys() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    String lowerSql = sql.toLowerCase();
    assertTrue(sql.contains("CREATE TABLE dish_template_categories"));
    assertTrue(sql.contains("CREATE TABLE dish_templates"));
    assertTrue(sql.contains("CREATE TABLE dish_template_ingredients"));
    assertTrue(sql.contains("CREATE TABLE dish_template_source_records"));
    assertTrue(sql.contains("CREATE TABLE dish_template_name_aliases"));
    assertTrue(sql.contains("CREATE TABLE dish_template_cooking_steps"));
    assertTrue(sql.contains("CREATE TABLE dish_template_image_assets"));
    assertTrue(sql.contains("ALTER TABLE dish_cooking_steps"));
    assertFalse(lowerSql.contains("foreign key"));
    assertFalse(lowerSql.contains(" references "));
  }

  @Test
  void migrationSeedsTenCategoriesAndTwoHundredFortyLegacyTemplates() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertEquals(10, countLines(sql, "INSERT INTO dish_template_categories "));
    assertEquals(240, countLines(sql, "INSERT INTO dish_templates "));
    assertTrue(countLines(sql, "INSERT INTO dish_template_ingredients ") >= 282);
    assertEquals(240, countLines(sql, "-- TEMPLATE "));
    assertTrue(sql.contains("'豆角焖面'"));
    assertTrue(sql.contains("'广式煲仔饭'"));
    assertTrue(sql.contains("'COMPONENT','配料组件'"));
  }

  @Test
  void templatePriceAndPublicImageFieldsAreNullable() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertTrue(sql.contains("image_url varchar(500) NULL"));
    assertTrue(sql.contains("image_source_url varchar(1000) NULL"));
    assertTrue(sql.contains("image_author varchar(255) NULL"));
    assertTrue(sql.contains("image_license varchar(255) NULL"));
    assertTrue(sql.contains("reference_price decimal(10,2) NULL"));
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
