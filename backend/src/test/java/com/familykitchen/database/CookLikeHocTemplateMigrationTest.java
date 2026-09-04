package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证 CookLikeHOC 模板同步所需的最终 V4 数据模型契约。 */
class CookLikeHocTemplateMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V4__init_dish_template_market.sql");

  @Test
  void schemaTracksSourcesAliasesStepsImagesAndQuantityState() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertTrue(sql.contains("template_type varchar(20)"));
    assertTrue(sql.contains("source_type varchar(30)"));
    assertTrue(sql.contains("source_key varchar(500) NULL"));
    assertTrue(sql.contains("data_status varchar(30)"));
    assertTrue(sql.contains("procurement_ready tinyint(1)"));
    assertTrue(sql.contains("image_rights_status varchar(20)"));
    assertTrue(sql.contains("quantity_status varchar(30)"));
    assertTrue(sql.contains("component_template_id bigint NULL"));
    assertTrue(sql.contains("component_occurrence_key varchar(500) NULL"));
    assertTrue(sql.contains("CREATE TABLE dish_template_source_records"));
    assertTrue(sql.contains("CREATE TABLE dish_template_name_aliases"));
    assertTrue(sql.contains("CREATE TABLE dish_template_cooking_steps"));
    assertTrue(sql.contains("CREATE TABLE dish_template_image_assets"));
    assertTrue(sql.contains("rejection_reason varchar(500) NULL"));
  }

  @Test
  void schemaEnforcesImageAndIngredientStateInvariants() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertTrue(sql.contains("CONSTRAINT chk_template_image_rights"));
    assertTrue(sql.contains("CONSTRAINT chk_template_ingredient_quantity_state"));
    assertTrue(sql.contains("CONSTRAINT chk_template_image_asset_rejection"));
    assertFalse(sql.toLowerCase().contains("foreign key"));
    assertFalse(sql.toLowerCase().contains(" references "));
  }

  @Test
  void migrationHasExactlyOneCompleteGeneratedDataSection() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertEquals(1, count(sql, "-- BEGIN GENERATED COOKLIKEHOC DATA"));
    assertEquals(1, count(sql, "-- END GENERATED COOKLIKEHOC DATA"));
    int start = sql.indexOf("-- BEGIN GENERATED COOKLIKEHOC DATA")
        + "-- BEGIN GENERATED COOKLIKEHOC DATA".length();
    int end = sql.indexOf("-- END GENERATED COOKLIKEHOC DATA");
    String generated = sql.substring(start, end);
    assertFalse(generated.isBlank());
    assertEquals(336, count(generated, "INSERT INTO dish_template_source_records"));
    assertEquals(179, count(generated, "INSERT INTO dish_template_image_assets"));
    assertTrue(generated.contains("f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed"));
    assertTrue(generated.contains("INSERT INTO dish_template_cooking_steps"));
  }

  private static int count(String text, String value) {
    return (text.length() - text.replace(value, "").length()) / value.length();
  }
}
