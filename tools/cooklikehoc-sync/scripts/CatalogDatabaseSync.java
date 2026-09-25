import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.StringJoiner;
import java.util.zip.CRC32;

/** Applies a reviewed recipe sync and exports the resulting catalog as the clean V3 baseline. */
public final class CatalogDatabaseSync {
  private static final List<TableSpec> TABLES = List.of(
      new TableSpec("dish_template_categories", "id,code,name,sort_order,enabled"),
      new TableSpec("dish_templates", "id,template_code,category_id,name,description,image_url,"
          + "image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,"
          + "template_type,source_type,source_key,source_url,source_revision,source_category,"
          + "source_yield_text,data_status,procurement_ready,image_rights_status,sort_order,enabled,version"),
      new TableSpec("dish_template_ingredients", "id,template_id,ingredient_name,ingredient_category,"
          + "quantity,unit,calc_type,source_text,source_quantity_text,quantity_status,component_template_id,"
          + "source_line_key,component_occurrence_key,component_multiplier,sort_order"),
      new TableSpec("dish_template_cooking_steps", "id,item_key,template_id,step_no,title,content,"
          + "source_text,duration_seconds,temperature_text,heat_level,component_template_id"),
      new TableSpec("dish_template_source_records", "id,template_id,source_key,source_title,"
          + "source_category,source_path,source_url,source_revision,record_type,alias_reason,content_sha256"),
      new TableSpec("dish_template_image_assets", "id,template_id,source_record_id,internal_storage_key,"
          + "content_sha256,mime_type,file_size,source_image_path,source_url,source_revision,asset_status,"
          + "public_image_url,image_author,image_license,reviewed_by,reviewed_at,rejection_reason")
  );

  private CatalogDatabaseSync() { }

  public static void main(String[] args) throws Exception {
    if (args.length != 7) {
      throw new IllegalArgumentException("Usage: <jdbcUrl> <username> <password> <generatedSql> "
          + "<backupSql> <baselineSql> <sourceRevision>");
    }
    boolean exportOnly = "--export-only".equals(args[3]);
    Path generatedSql = exportOnly ? null : Path.of(args[3]).toAbsolutePath();
    Path backupSql = Path.of(args[4]).toAbsolutePath();
    Path baselineSql = Path.of(args[5]).toAbsolutePath();
    String sourceRevision = args[6];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection connection = DriverManager.getConnection(args[0], args[1], args[2])) {
      if (!exportOnly) {
        Files.createDirectories(backupSql.getParent());
        exportCatalog(connection, backupSql, "同步前模板目录备份", currentRevision(connection));
        applySync(connection, generatedSql);
      }
      verify(connection, sourceRevision);
      exportCatalog(connection, baselineSql, "完整模板菜谱目录", sourceRevision);
      repairV3Checksum(connection, baselineSql);
      printCounts(connection);
    }
  }

  private static void applySync(Connection connection, Path generatedSql) throws Exception {
    connection.setAutoCommit(false);
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate("DELETE FROM dish_template_image_assets");
      statement.executeUpdate("DELETE FROM dish_template_source_records");
      for (String line : Files.readAllLines(generatedSql, StandardCharsets.UTF_8)) {
        String sql = line.trim();
        if (sql.isEmpty() || sql.startsWith("--")) continue;
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1);
        if (sql.startsWith("INSERT INTO dish_templates ")) {
          sql += " ON DUPLICATE KEY UPDATE template_code=VALUES(template_code),"
              + "category_id=VALUES(category_id),name=VALUES(name),description=VALUES(description),"
              + "image_url=VALUES(image_url),image_source_url=VALUES(image_source_url),"
              + "image_author=VALUES(image_author),image_license=VALUES(image_license),"
              + "reference_price=VALUES(reference_price),taste_tags=VALUES(taste_tags),"
              + "meal_tags=VALUES(meal_tags),template_type=VALUES(template_type),"
              + "source_type=VALUES(source_type),source_key=VALUES(source_key),"
              + "source_url=VALUES(source_url),source_revision=VALUES(source_revision),"
              + "source_category=VALUES(source_category),source_yield_text=VALUES(source_yield_text),"
              + "data_status=VALUES(data_status),procurement_ready=VALUES(procurement_ready),"
              + "image_rights_status=VALUES(image_rights_status),sort_order=VALUES(sort_order),"
              + "enabled=VALUES(enabled)";
        }
        statement.executeUpdate(sql);
      }
      connection.commit();
    } catch (Exception exception) {
      connection.rollback();
      throw exception;
    } finally {
      connection.setAutoCommit(true);
    }
  }

  private static void verify(Connection connection, String sourceRevision) throws Exception {
    assertCount(connection, "SELECT COUNT(*) FROM dish_templates", 552);
    assertCount(connection, "SELECT COUNT(*) FROM dish_template_source_records", 339);
    assertCount(connection, "SELECT COUNT(*) FROM dish_template_cooking_steps", 807);
    assertCount(connection, "SELECT COUNT(*) FROM dish_template_source_records WHERE source_revision=?",
        339, sourceRevision);
    for (String name : List.of("红米糙米红薯饭", "山药莲子鸡汤", "金秋板栗烧鸡")) {
      assertCount(connection, "SELECT COUNT(*) FROM dish_templates WHERE name=?", 1, name);
    }
  }

  private static void assertCount(Connection connection, String sql, long expected,
      Object... parameters) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < parameters.length; index++) {
        statement.setObject(index + 1, parameters[index]);
      }
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        long actual = result.getLong(1);
        if (actual != expected) {
          throw new IllegalStateException("Catalog verification failed: expected " + expected
              + " but got " + actual + " for " + sql);
        }
      }
    }
  }

  private static String currentRevision(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery(
             "SELECT source_revision FROM dish_template_source_records ORDER BY id LIMIT 1")) {
      return result.next() ? result.getString(1) : "none";
    }
  }

  private static void exportCatalog(Connection connection, Path target, String title,
      String sourceRevision) throws Exception {
    StringBuilder sql = new StringBuilder()
        .append("-- ").append(title).append("。\n")
        .append("-- 来源版本：").append(sourceRevision).append("。\n\n")
        .append("SET NAMES utf8mb4;\n\n")
        .append("-- BEGIN GENERATED COOKLIKEHOC DATA\n");
    for (TableSpec table : TABLES) {
      try (Statement statement = connection.createStatement();
           ResultSet rows = statement.executeQuery(
               "SELECT " + table.columns() + " FROM " + table.name() + " ORDER BY id")) {
        ResultSetMetaData metadata = rows.getMetaData();
        while (rows.next()) {
          StringJoiner values = new StringJoiner(",");
          for (int index = 1; index <= metadata.getColumnCount(); index++) {
            values.add(toSql(rows.getObject(index)));
          }
          sql.append("INSERT INTO ").append(table.name()).append(" (").append(table.columns())
              .append(") VALUES (").append(values).append(");\n");
        }
      }
      sql.append('\n');
    }
    sql.append("-- END GENERATED COOKLIKEHOC DATA\n");
    Files.writeString(target, sql.toString(), StandardCharsets.UTF_8);
  }

  private static String toSql(Object value) {
    if (value == null) return "NULL";
    if (value instanceof Number number) {
      return number instanceof BigDecimal decimal ? decimal.toPlainString() : number.toString();
    }
    if (value instanceof Boolean bool) return bool ? "1" : "0";
    if (value instanceof TemporalAccessor) return quote(value.toString());
    return quote(value.toString());
  }

  private static String quote(String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "''")
        .replace("\r", "\\r").replace("\n", "\\n") + "'";
  }

  private static void repairV3Checksum(Connection connection, Path baselineSql) throws Exception {
    CRC32 crc32 = new CRC32();
    try (BufferedReader reader = Files.newBufferedReader(baselineSql, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) crc32.update(line.getBytes(StandardCharsets.UTF_8));
    }
    int checksum = (int) crc32.getValue();
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE flyway_schema_history SET checksum=? WHERE version='3' AND success=1")) {
      statement.setInt(1, checksum);
      if (statement.executeUpdate() != 1) {
        throw new IllegalStateException("Unable to repair applied Flyway V3 checksum");
      }
    }
  }

  private static void printCounts(Connection connection) throws Exception {
    for (String table : List.of("dish_templates", "dish_template_ingredients",
        "dish_template_cooking_steps", "dish_template_source_records", "dish_template_image_assets")) {
      try (Statement statement = connection.createStatement();
           ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
        result.next();
        System.out.println(table + "=" + result.getLong(1));
      }
    }
  }

  private record TableSpec(String name, String columns) { }
}
