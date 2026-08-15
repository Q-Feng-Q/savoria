package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** 验证全新空库迁移脚本结构、无外键约束和基础数据边界。 */
class FreshDatabaseMigrationTest {

  private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
  private static final Pattern CREATE_TABLE = Pattern.compile(
      "CREATE TABLE\\s+([a-z][a-z0-9_]*)\\s*\\((.*?)\\) ENGINE=", Pattern.DOTALL);
  private static final Pattern INDEX = Pattern.compile(
      "(?m)^\\s*(?:UNIQUE\\s+)?KEY\\s+[a-z][a-z0-9_]*\\s*\\(([^)]+)\\)");

  @Test
  void migrationsAreFreshOrderedAndContainNoPhysicalForeignKeys() throws Exception {
    List<Path> files;
    try (var stream = Files.list(MIGRATION_DIR)) {
      files = stream.filter(Files::isRegularFile).sorted().toList();
    }
    assertEquals(List.of(
        "V1__init_identity_family_and_merchant.sql",
        "V2__init_menu_order_wallet_and_purchase.sql",
        "V3__init_system_notification_and_defaults.sql",
        "V4__init_dish_template_market.sql",
        "V5__expand_regional_dish_templates.sql",
        "V6__finalize_regional_dish_template_images.sql"),
        files.stream().map(path -> path.getFileName().toString()).toList());

    String sql = readAll(files).toLowerCase();
    assertFalse(sql.contains("foreign key"));
    assertFalse(sql.contains(" references "));
    assertFalse(sql.contains("create table members"));
    assertFalse(sql.contains("create table admin_accounts"));
    assertFalse(sql.contains("legacy_user_mappings"));
    assertFalse(sql.contains("submitter_member_id"));
    assertFalse(sql.contains("owner_member_id"));
    assertFalse(sql.contains("delivery_fee_payer_member_id"));

  }

  @Test
  void migrationsUseUnifiedUserColumnsAndRequiredInitialData() throws Exception {
    List<Path> files;
    try (var stream = Files.list(MIGRATION_DIR)) {
      files = stream.filter(Files::isRegularFile).sorted().toList();
    }
    String sql = readAll(files);
    assertTrue(sql.contains("submitter_user_id"));
    assertTrue(sql.contains("owner_user_id"));
    assertTrue(sql.contains("CREATE TABLE family_user_relations"));
    assertFalse(sql.contains("CREATE TABLE default_dish_categories"));
    assertFalse(sql.contains("CREATE TABLE default_merchant_ingredients"));
    assertTrue(sql.contains("INSERT INTO merchants"));
    assertTrue(sql.contains("INSERT INTO merchant_user_relations"));
    assertTrue(sql.contains("'家常热菜'"));
    assertTrue(sql.contains("'西红柿'"));
    assertFalse(sql.toLowerCase().contains("insert into families"));
    assertTrue(sql.contains("INSERT INTO users"));
    assertTrue(sql.contains("INSERT INTO user_role_relations"));
  }

  @Test
  void everySeedRecordUsesAnIndependentSqlLineAndAdminPasswordIsUsable() throws Exception {
    String sql = Files.readString(
        MIGRATION_DIR.resolve("V3__init_system_notification_and_defaults.sql"), StandardCharsets.UTF_8);
    List<String> insertLines = sql.lines()
        .map(String::trim)
        .filter(line -> line.startsWith("INSERT INTO "))
        .toList();
    assertTrue(insertLines.stream().allMatch(line -> line.endsWith(";")),
        "每条初始化数据必须使用一条完整且独立的 INSERT 语句");
    assertEquals(10, insertLines.stream()
        .filter(line -> line.startsWith("INSERT INTO dish_categories ")).count());
    assertEquals(73, insertLines.stream()
        .filter(line -> line.startsWith("INSERT INTO merchant_ingredients ")).count());
    assertTrue(insertLines.stream()
        .filter(line -> line.startsWith("INSERT INTO dish_categories "))
        .allMatch(line -> line.contains("(merchant_id,name,sort_order,enabled)")
            && line.contains("VALUES (1,")));
    assertTrue(insertLines.stream()
        .filter(line -> line.startsWith("INSERT INTO merchant_ingredients "))
        .allMatch(line -> line.contains("(merchant_id,name,category,unit)")
            && line.contains("VALUES (1,")));
    assertEquals(1, insertLines.stream().filter(line -> line.startsWith("INSERT INTO users ")).count());
    assertEquals(1, insertLines.stream()
        .filter(line -> line.startsWith("INSERT INTO user_role_relations ")).count());
    assertEquals(1, insertLines.stream().filter(line -> line.startsWith("INSERT INTO merchants ")).count());
    assertEquals(1, insertLines.stream()
        .filter(line -> line.startsWith("INSERT INTO merchant_user_relations ")).count());

    Matcher adminHash = Pattern.compile(
        "INSERT INTO users .*?VALUES \\(1,'admin',0,'([^']+)'", Pattern.CASE_INSENSITIVE)
        .matcher(sql);
    assertTrue(adminHash.find(), "初始化脚本必须写入固定 admin 账号");
    assertTrue(new BCryptPasswordEncoder().matches("123456", adminHash.group(1)),
        "admin 初始化密码摘要必须对应默认密码 123456");
  }

  @Test
  void everyTableAndColumnHasChineseDatabaseDocumentation() throws Exception {
    try (var stream = Files.list(MIGRATION_DIR)) {
      for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
        boolean insideTable = false;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
          String sqlLine = line.trim();
          if (sqlLine.startsWith("CREATE TABLE ")) {
            insideTable = true;
            continue;
          }
          if (!insideTable) continue;
          if (sqlLine.startsWith(") ENGINE=")) {
            assertTrue(sqlLine.contains("COMMENT='"), file + " 存在缺少表注释的建表语句");
            insideTable = false;
            continue;
          }
          if (sqlLine.matches("[a-z][a-z0-9_]*\\s+.*")) {
            assertTrue(sqlLine.contains("COMMENT '"), file + " 存在缺少字段注释的字段：" + sqlLine);
          }
        }
        assertFalse(insideTable, file + " 存在未结束的建表语句");
      }
    }
  }

  @Test
  void everyIndexOnlyReferencesColumnsDeclaredInItsTable() throws Exception {
    String sql;
    try (var stream = Files.list(MIGRATION_DIR)) {
      sql = readAll(stream.filter(Files::isRegularFile).sorted().toList());
    }
    Matcher tableMatcher = CREATE_TABLE.matcher(sql);
    while (tableMatcher.find()) {
      String tableName = tableMatcher.group(1);
      String tableBody = tableMatcher.group(2);
      Set<String> columns = new HashSet<>();
      for (String line : tableBody.split("\\R")) {
        String definition = line.trim();
        if (definition.matches("[a-z][a-z0-9_]*\\s+.*")) {
          columns.add(definition.substring(0, definition.indexOf(' ')));
        }
      }
      Matcher indexMatcher = INDEX.matcher(tableBody);
      while (indexMatcher.find()) {
        for (String indexedExpression : indexMatcher.group(1).split(",")) {
          String indexedColumn = indexedExpression.trim().split("\\s+|\\(")[0];
          assertTrue(columns.contains(indexedColumn),
              tableName + " 的索引引用了未声明字段：" + indexedColumn);
        }
      }
    }
  }

  private static String readAll(List<Path> files) throws Exception {
    StringBuilder sql = new StringBuilder();
    for (Path file : files) sql.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
    return sql.toString();
  }
}
