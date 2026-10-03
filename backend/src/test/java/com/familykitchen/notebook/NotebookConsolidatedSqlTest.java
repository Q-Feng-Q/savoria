package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/** Confirms the manual one-file install matches the forward-only Flyway migrations. */
class NotebookConsolidatedSqlTest {
  @Test void oneFileFreshInstallHasTheSameFinalSchemaAsV5ThroughV8() throws Exception {
    try (Connection consolidated = database(); Connection incremental = database()) {
      prepareBase(consolidated);
      prepareBase(incremental);
      apply(consolidated, "/db/notebook/personal_notebook_full.sql");
      for (String migration : new String[] {
          "/db/migration/V5__personal_notebook.sql",
          "/db/migration/V6__notebook_image_cleanup_queue.sql",
          "/db/migration/V7__notebook_grant_data_zone.sql",
          "/db/migration/V8__notebook_staged_images.sql"}) {
        apply(incremental, migration);
      }
      assertThat(columns(consolidated)).isEqualTo(columns(incremental));
      assertThat(columns(consolidated)).contains("SYSTEM_SETTINGS.NOTEBOOK_MAX_QUERY_MONTHS",
          "NOTEBOOK_GRANTS.DATA_TIME_ZONE", "NOTEBOOK_STAGED_IMAGES.STORAGE_KEY",
          "NOTEBOOK_IMAGE_CLEANUP.STORAGE_KEY");
    }
  }

  private Connection database() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:notebook_full_" + UUID.randomUUID()
        + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    return source.getConnection();
  }

  private void prepareBase(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
      statement.execute("CREATE TABLE system_settings (id BIGINT PRIMARY KEY)");
    }
  }

  private void apply(Connection connection, String resource) throws Exception {
    try (InputStream stream = getClass().getResourceAsStream(resource)) {
      assertThat(stream).as(resource).isNotNull();
      String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
          .replaceAll("(?m)^\\s*--[^\\r\\n]*$", "");
      try (Statement statement = connection.createStatement()) {
        for (String command : sql.split(";")) {
          if (!command.isBlank()) statement.execute(command);
        }
      }
    }
  }

  private Set<String> columns(Connection connection) throws Exception {
    Set<String> output = new TreeSet<>();
    DatabaseMetaData metadata = connection.getMetaData();
    try (var tables = metadata.getTables(null, null, null, new String[] {"TABLE"})) {
      while (tables.next()) {
        String table = tables.getString("TABLE_NAME");
        if (!table.startsWith("NOTEBOOK_") && !table.equals("SYSTEM_SETTINGS")) continue;
        try (var fields = metadata.getColumns(null, null, table, null)) {
          while (fields.next()) output.add(table + "." + fields.getString("COLUMN_NAME"));
        }
      }
    }
    return output;
  }
}
