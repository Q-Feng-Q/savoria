package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证完整菜谱目录已经并入空库初始化基线。 */
class CookLikeHocForwardMigrationTest {

  private static final Path SCHEMA = Path.of(
      "src/main/resources/db/migration/V1__init_schema.sql");
  private static final Path CATALOG = Path.of(
      "src/main/resources/db/migration/V3__init_recipe_catalog.sql");

  @Test
  void baselineCreatesFinalSchemaWithoutIncrementalAlterStatements() throws Exception {
    String sql = Files.readString(SCHEMA, StandardCharsets.UTF_8);
    String lowerSql = sql.toLowerCase();

    assertTrue(sql.contains("CREATE TABLE dish_templates"));
    assertTrue(sql.contains("description varchar(255) DEFAULT NULL"));
    assertTrue(sql.contains("image_url varchar(500) DEFAULT NULL"));
    assertTrue(sql.contains("CREATE TABLE dish_template_source_records"));
    assertTrue(sql.contains("CREATE TABLE dish_template_cooking_steps"));
    assertTrue(sql.contains("ingredient_name varchar(255) NOT NULL"));
    assertFalse(lowerSql.contains("alter table"));
    assertFalse(lowerSql.contains("foreign key"));
    assertFalse(lowerSql.contains(" references "));
  }

  @Test
  void migrationContainsTheReviewedCompleteCatalog() throws Exception {
    String sql = Files.readString(CATALOG, StandardCharsets.UTF_8);

    assertEquals(549, countLines(sql, "INSERT INTO dish_templates "));
    assertEquals(336, countLines(sql, "INSERT INTO dish_template_source_records "));
    assertEquals(1987, countLines(sql, "INSERT INTO dish_template_ingredients "));
    assertEquals(795, countLines(sql, "INSERT INTO dish_template_cooking_steps "));
    assertEquals(179, countLines(sql, "INSERT INTO dish_template_image_assets "));
    assertTrue(sql.contains("'Q 弹虾滑馄饨'"));
    assertTrue(sql.contains("'f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed'"));
  }

  @Test
  void imageAssetUniqueIndexFitsTheInnoDbUtf8mb4KeyLimit() throws Exception {
    String sql = Files.readString(SCHEMA, StandardCharsets.UTF_8);

    assertTrue(sql.contains(
        "(template_id,source_revision,source_image_path(512),content_sha256)"));
    assertFalse(sql.contains(
        "(template_id,source_revision,source_image_path,content_sha256)"));
  }

  private static long countLines(String sql, String prefix) {
    return sql.lines().map(String::trim).filter(line -> line.startsWith(prefix)).count();
  }
}
