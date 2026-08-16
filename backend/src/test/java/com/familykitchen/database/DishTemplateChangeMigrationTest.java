package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 验证平台模板菜品修改审核的 V8 数据库迁移契约。 */
class DishTemplateChangeMigrationTest {

  private static final Path MIGRATION = Path.of(
      "src/main/resources/db/migration/V8__add_dish_template_change_review.sql");

  @Test
  void migrationAddsTemplateVersionAndCompleteReviewTable() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertTrue(sql.contains("ALTER TABLE dish_templates"));
    assertTrue(sql.contains("ADD COLUMN version bigint NOT NULL DEFAULT 0 COMMENT '模板并发版本号'"));
    assertTrue(sql.contains("CREATE TABLE dish_template_change_requests"));
    assertTrue(sql.contains("base_snapshot_json json NOT NULL COMMENT '提交时模板完整业务快照'"));
    assertTrue(sql.contains("snapshot_json json NOT NULL COMMENT '申请覆盖后的完整业务快照'"));
    assertTrue(sql.contains("submitted_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"));
    assertTrue(sql.contains("reviewed_at datetime(6) NULL"));
    assertTrue(sql.contains("withdrawn_at datetime(6) NULL"));
  }

  @Test
  void migrationUsesPendingGeneratedColumnAndRequiredIndexes() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertTrue(sql.contains("pending_marker tinyint GENERATED ALWAYS AS"));
    assertTrue(sql.contains("CASE WHEN status='PENDING' THEN 1 ELSE NULL END"));
    assertTrue(sql.contains("CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN'))"));
    assertTrue(sql.contains("UNIQUE KEY uk_dish_template_change_pending (merchant_id,template_id,pending_marker)"));
    assertTrue(sql.contains("KEY idx_dish_template_change_status_time (status,submitted_at,id)"));
    assertTrue(sql.contains("KEY idx_dish_template_change_merchant_time (merchant_id,status,submitted_at,id)"));
    assertTrue(sql.contains("KEY idx_dish_template_change_template_time (template_id,status,submitted_at,id)"));
  }

  @Test
  void migrationIsDocumentedUtf8AndHasNoPhysicalForeignKeys() throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
    String lowerSql = sql.toLowerCase();

    assertTrue(sql.contains("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci"));
    assertTrue(sql.contains("COMMENT='平台模板菜品修改审核申请'"));
    assertFalse(lowerSql.contains("foreign key"));
    assertFalse(lowerSql.contains(" references "));
  }
}
