package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** 验证商户特色菜字段、菜单结构和模型契约。 */
class MerchantFeaturedDishMigrationContractTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V1__init_schema.sql");

  @Test
  void schemaDeclaresMerchantFeaturedTimestampAndIndex() throws Exception {
    String sql = normalizedSql();

    assertTrue(sql.contains("featured_at datetime(6) default null comment '商户推荐时间'"));
    assertTrue(sql.contains("key idx_dishes_merchant_featured (merchant_id,featured_at)"));
  }

  @Test
  void schemaKeepsFamilyMenuCustomizationFields() throws Exception {
    String sql = normalizedSql();

    assertTrue(sql.contains("create table family_menu_items ("));
    assertTrue(sql.contains("enabled tinyint(1) not null default '1'"));
    assertTrue(sql.contains("sort_order int not null default '0'"));
    assertTrue(sql.contains("final_price decimal(10,2) default null"));
  }

  @Test
  void modelsExposeFeaturedTimestampAndDerivedFlag() throws Exception {
    assertEquals(LocalDateTime.class, DishEntity.class.getDeclaredField("featuredAt").getType());
    assertRecordComponent(DishView.class, "featuredAt", LocalDateTime.class);
    assertRecordComponent(DishView.class, "featured", boolean.class);
    assertRecordComponent(FamilyMenuItemView.class, "featuredAt", LocalDateTime.class);
    assertRecordComponent(FamilyMenuItemView.class, "featured", boolean.class);
  }

  @Test
  void merchantDishMapperSelectsAndDeterministicallySortsFeaturedMetadata() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/mapper/dish/DishMapper.xml"),
            StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ")
        .toLowerCase();

    assertTrue(xml.contains("<result property=\"featuredat\" column=\"featured_at\"/>"));
    assertTrue(xml.contains("d.source_template_id, d.featured_at, d.status, d.deleted_at"));
    assertTrue(xml.contains(
        "(d.featured_at is not null) desc, d.featured_at desc, d.id desc"));
  }

  private static String normalizedSql() throws Exception {
    return Files.readString(MIGRATION, StandardCharsets.UTF_8)
        .replaceAll("\\s*,\\s*", ",")
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase();
  }

  private static void assertRecordComponent(Class<?> type, String name, Class<?> componentType) {
    assertTrue(Arrays.stream(type.getRecordComponents())
        .anyMatch(component -> component.getName().equals(name)
            && component.getType().equals(componentType)),
        () -> type.getSimpleName() + " must expose " + componentType.getSimpleName() + " " + name);
  }
}
