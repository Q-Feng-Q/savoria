package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证地方特色菜模板增量数据可安全追加到现有模板市场。 */
class DishTemplateRegionalExpansionTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V5__expand_regional_dish_templates.sql");

  @Test
  void migrationAddsFortyTwoRegionalDishesWithIngredientsAndLocalImages() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    assertEquals(42, countLines(sql, "INSERT INTO dish_templates "));
    assertTrue(countLines(sql, "INSERT INTO dish_template_ingredients ") >= 84);
    assertTrue(sql.contains("'豆角焖面'"));
    assertTrue(sql.contains("'武汉热干面'"));
    assertTrue(sql.contains("'东北锅包肉'"));
    assertTrue(sql.contains("'客家酿豆腐'"));
    assertTrue(sql.contains("'广式煲仔饭'"));
    assertTrue(sql.contains("'赛螃蟹'"));
    assertFalse(sql.toLowerCase().contains("foreign key"));
    assertFalse(sql.toLowerCase().contains(" references "));
    for (int id = 199; id <= 240; id++) {
      assertTrue(Files.isRegularFile(Path.of(
          "src/main/resources/static/images/dish-templates/dish-%03d.jpg".formatted(id))),
          "缺少特色菜图片 DISH_%03d".formatted(id));
    }
  }

  private static long countLines(String sql, String prefix) {
    return sql.lines().map(String::trim).filter(line -> line.startsWith(prefix)).count();
  }
}
