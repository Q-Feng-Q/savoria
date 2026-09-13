package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证完整菜谱目录通过前向迁移兼容已经执行历史 V4/V5/V6 的数据库。 */
class CookLikeHocForwardMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V13__sync_complete_cooklikehoc_recipe_catalog.sql");

  @Test
  void migrationUpgradesExistingSchemaWithoutChangingHistoricalMigrations() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    String lowerSql = sql.toLowerCase();

    assertTrue(sql.contains("ALTER TABLE dish_templates"));
    assertTrue(sql.contains("MODIFY COLUMN description varchar(255) NULL"));
    assertTrue(sql.contains("MODIFY COLUMN image_url varchar(500) NULL"));
    assertTrue(sql.contains("CREATE TABLE dish_template_source_records"));
    assertTrue(sql.contains("CREATE TABLE dish_template_cooking_steps"));
    assertTrue(sql.contains("ALTER TABLE dish_cooking_steps"));
    assertTrue(sql.contains("MODIFY COLUMN ingredient_name varchar(255) NOT NULL"));
    assertTrue(sql.contains("ALTER TABLE dish_ingredients"));
    assertTrue(sql.contains("ALTER TABLE purchase_list_items"));
    assertFalse(sql.contains("ALTER TABLE purchase_items"));
    assertTrue(sql.contains("ALTER TABLE temp_purchase_items"));
    assertFalse(lowerSql.contains("foreign key"));
    assertFalse(lowerSql.contains(" references "));
  }

  @Test
  void migrationContainsTheReviewedCompleteCatalog() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertEquals(309, countLines(sql, "INSERT INTO dish_templates "));
    assertEquals(336, countLines(sql, "INSERT INTO dish_template_source_records "));
    assertEquals(1618, countLines(sql, "INSERT INTO dish_template_ingredients "));
    assertEquals(795, countLines(sql, "INSERT INTO dish_template_cooking_steps "));
    assertEquals(179, countLines(sql, "INSERT INTO dish_template_image_assets "));
    assertTrue(sql.contains("'Q 弹虾滑馄饨'"));
    assertTrue(sql.contains("'f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed'"));
  }

  @Test
  void imageAssetUniqueIndexFitsTheInnoDbUtf8mb4KeyLimit() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertTrue(sql.contains(
        "(template_id,source_revision,source_image_path(512),content_sha256)"));
    assertFalse(sql.contains(
        "(template_id,source_revision,source_image_path,content_sha256)"));
  }

  private static long countLines(String sql, String prefix) {
    return sql.lines().map(String::trim).filter(line -> line.startsWith(prefix)).count();
  }
}
