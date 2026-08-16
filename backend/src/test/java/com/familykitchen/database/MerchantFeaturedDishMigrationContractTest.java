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

/** Verifies the V10 merchant-level featured-dish migration and model contract. */
class MerchantFeaturedDishMigrationContractTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V10__add_merchant_featured_dishes.sql");

  @Test
  void migrationAddsMerchantFeaturedTimestampAndDeterministicTopFiveBackfill() throws Exception {
    String sql = normalizedSql();

    assertTrue(sql.contains("add column featured_at datetime(6) null comment '商户推荐时间'"));
    assertTrue(sql.contains("key idx_dishes_merchant_featured (merchant_id,featured_at)"));
    assertTrue(sql.contains("count(distinct f.id) as family_reference_count"));
    assertTrue(sql.contains("partition by candidate.merchant_id"));
    assertTrue(sql.contains("order by candidate.family_reference_count desc,candidate.dish_id desc"));
    assertTrue(sql.contains("where ranked.featured_rank <= 5"));
    assertTrue(sql.contains("f.status = 'active'"));
    assertTrue(sql.contains("d.status = 'active'"));
    assertTrue(sql.contains("d.merchant_id = f.merchant_id"));
  }

  @Test
  void migrationEnablesEveryActiveFamilyWithoutOverwritingExistingMenuCustomization() throws Exception {
    String sql = normalizedSql();

    assertTrue(sql.contains("update family_menu_items fmi"));
    assertTrue(sql.contains("set fmi.enabled = 1"));
    assertTrue(sql.contains("insert into family_menu_items (family_id,dish_id,enabled,sort_order,final_price)"));
    assertTrue(sql.contains("where f.status = 'active'"));
    assertTrue(sql.contains("not exists"));
    assertTrue(sql.contains("existing_tail.max_sort_order"));
    assertTrue(sql.contains("row_number() over ( partition by f.id order by d.featured_at desc,d.id desc )"));
    assertTrue(sql.contains("d.base_price"));
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
    assertTrue(xml.contains("source_template_id, featured_at, status"));
    assertTrue(xml.contains("order by (featured_at is not null) desc, featured_at desc, id desc"));
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
