package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.dish.model.entity.DishEntity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 验证菜品逻辑删除字段已经进入最终初始化结构。 */
class DishLogicalDeletionMigrationContractTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V1__init_schema.sql");

  @Test
  void migrationAddsNullableAuditFieldsAndDeterministicScopeIndex() throws Exception {
    assertTrue(Files.exists(MIGRATION), "V1 初始化结构必须存在");
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8)
        .replaceAll("\\s*,\\s*", ",")
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase();

    assertTrue(sql.contains("deleted_at datetime(6) default null"));
    assertTrue(sql.contains("deleted_by bigint default null"));
    assertTrue(sql.contains("key idx_dishes_merchant_status_deleted (merchant_id,status,deleted_at,id)"));
  }

  @Test
  void entityExposesDeletionAuditFields() throws Exception {
    assertEquals(LocalDateTime.class, DishEntity.class.getDeclaredField("deletedAt").getType());
    assertEquals(Long.class, DishEntity.class.getDeclaredField("deletedBy").getType());
    assertEquals(String.class, DishEntity.class.getDeclaredField("deletedByName").getType());
  }

  @Test
  void mapperMapsAuditFieldsAndSeparatesAvailableFromDeletedScopes() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/mapper/dish/DishMapper.xml"),
            StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ")
        .toLowerCase();

    assertTrue(xml.contains("property=\"deletedat\" column=\"deleted_at\""));
    assertTrue(xml.contains("property=\"deletedby\" column=\"deleted_by\""));
    assertTrue(xml.contains("property=\"deletedbyname\" column=\"deleted_by_name\""));
    assertTrue(xml.contains("d.status &lt;&gt; 'deleted'"));
    assertTrue(xml.contains("d.status = 'deleted'"));
  }
}
