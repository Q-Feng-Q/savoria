package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/** Verifies the forward-only notebook schema contract. */
class NotebookSchemaTest {
  @Test void createsAccountOwnedNotebookTablesWithoutFamilyOwnership() throws Exception {
    try (InputStream stream = getClass().getResourceAsStream(
        "/db/migration/V5__personal_notebook.sql")) {
      assertThat(stream).isNotNull();
      String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
      for (String table : new String[] {"notebook_events", "notebook_template_versions",
          "notebook_records", "notebook_record_revisions", "notebook_contacts",
          "notebook_contact_invites", "notebook_grants", "notebook_audit", "notebook_images"}) {
        assertThat(sql).contains("create table " + table);
      }
      assertThat(sql).contains("notebook_max_query_months int not null default 36")
          .contains("references users(id)")
          .contains("unique key uq_notebook_template_event_version")
          .doesNotContain("family_id");
      String audit = sql.substring(sql.indexOf("create table notebook_audit"),
          sql.indexOf("create table notebook_images"));
      assertThat(audit).doesNotContain("references notebook_events")
          .doesNotContain("references notebook_records");
    }
  }

  @Test void migrationAppliesToMySqlCompatibleTestDatabase() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:notebook_" + java.util.UUID.randomUUID()
        + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    try (var connection = source.getConnection(); var statement = connection.createStatement();
        var stream = getClass().getResourceAsStream("/db/migration/V5__personal_notebook.sql")) {
      statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
      statement.execute("CREATE TABLE system_settings (id BIGINT PRIMARY KEY)");
      for (String command : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
        if (!command.isBlank()) statement.execute(command);
      }
      assertThat(connection.getMetaData().getTables(null, null, "NOTEBOOK_RECORDS", null).next())
          .isTrue();
      statement.execute("INSERT INTO users(id) VALUES (1),(2)");
      statement.execute("INSERT INTO notebook_events(id,owner_user_id,name) VALUES (10,1,'Journal')");
      statement.execute("INSERT INTO notebook_template_versions(event_id,version,fields_json,published_by_user_id) "
          + "VALUES (10,1,'{}',1)");
      statement.execute("INSERT INTO notebook_records(event_id,owner_user_id,created_by_user_id,"
          + "updated_by_user_id,occurred_from,occurred_to,title,template_version,values_json) "
          + "VALUES (10,1,1,1,'2026-01-01','2026-01-01','Valid',1,'{}')");
      assertThatThrownBy(() -> statement.execute("INSERT INTO notebook_records(event_id,owner_user_id,"
          + "created_by_user_id,updated_by_user_id,occurred_from,occurred_to,title,template_version,values_json) "
          + "VALUES (10,2,1,1,'2026-01-01','2026-01-01','Wrong owner',1,'{}')"))
          .isInstanceOf(java.sql.SQLException.class);
      statement.execute("INSERT INTO notebook_grants(event_id,owner_user_id,grantee_user_id,data_from,data_to,"
          + "valid_from,valid_to) VALUES (10,1,2,'2026-01-01','2026-01-31',"
          + "'2026-01-01','2026-02-01')");
      assertThatThrownBy(() -> statement.execute("INSERT INTO notebook_grants(event_id,owner_user_id,"
          + "grantee_user_id,data_from,data_to,valid_from,valid_to) VALUES "
          + "(10,2,1,'2026-01-01','2026-01-31','2026-01-01','2026-02-01')"))
          .isInstanceOf(java.sql.SQLException.class);
    }
  }
}
