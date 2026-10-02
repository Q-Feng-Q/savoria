package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.model.NotebookEventCreate;
import com.familykitchen.notebook.model.NotebookEventPatch;
import com.familykitchen.notebook.model.NotebookFieldInput;
import com.familykitchen.notebook.model.NotebookTemplateRequest;
import com.familykitchen.notebook.service.NotebookEventService;
import com.familykitchen.notebook.service.NotebookTemplateValidator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/** Account scoped event and immutable template integration tests. */
class NotebookEventServiceTest {
  private NotebookEventService service;
  private JdbcTemplate sql;
  private Path cleanupRoot;

  @AfterEach void removePrivateTestFiles() throws Exception {
    if (cleanupRoot == null) return;
    try (var files = Files.walk(cleanupRoot)) {
      for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
    }
  }

  @BeforeEach void database() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:event_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    sql = new JdbcTemplate(source);
    sql.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
    sql.execute("CREATE TABLE system_settings (id BIGINT PRIMARY KEY)");
    try (var stream = getClass().getResourceAsStream("/db/migration/V5__personal_notebook.sql")) {
      for (String command : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
        if (!command.isBlank()) sql.execute(command);
      }
    }
    sql.update("INSERT INTO users(id) VALUES (1),(2)");
    service = new NotebookEventService(new NotebookEventMapper(sql), new NotebookTemplateValidator(),
        new ObjectMapper());
  }

  @Test void createsAccountOwnedEventWithFirstTemplateAndDeniesOtherAccounts() {
    var created = service.create(1L, create("Journal"));
    assertThat(created.ownerUserId()).isEqualTo(1L);
    assertThat(created.currentTemplateVersion()).isEqualTo(1);
    assertThat(service.templates(1L, created.id())).hasSize(1);
    assertThat(service.list(2L, true)).isEmpty();
    assertThatThrownBy(() -> service.get(2L, created.id()))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    assertThatThrownBy(() -> service.publishTemplate(2L, created.id(),
        new NotebookTemplateRequest(List.of())))
        .isInstanceOf(BusinessException.class);
  }

  @Test void patchesArchiveFocusAndOwnerOrderWithoutChangingOwnership() {
    var first = service.create(1L, create("First"));
    var second = service.create(1L, create("Second"));
    service.update(1L, first.id(), new NotebookEventPatch("Renamed", null, null, true, true, null));
    assertThat(service.list(1L, false)).extracting(event -> event.id()).containsExactly(second.id());
    service.update(1L, first.id(), new NotebookEventPatch(null, null, null, null, false, null));
    assertThat(service.list(1L, false)).extracting(event -> event.id()).containsExactly(first.id(), second.id());
    service.update(1L, second.id(), new NotebookEventPatch(null, null, null, true, null, null));
    service.reorder(1L, List.of(second.id(), first.id()));
    assertThat(service.list(1L, false)).extracting(event -> event.id()).containsExactly(second.id(), first.id());
    assertThat(service.get(1L, first.id()).starred()).isTrue();
    assertThat(service.get(1L, first.id()).name()).isEqualTo("Renamed");
    assertThatThrownBy(() -> service.update(2L, first.id(),
        new NotebookEventPatch("Intrusion", null, null, null, null, null)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.reorder(2L, List.of(first.id())))
        .isInstanceOf(BusinessException.class);
  }

  @Test void publishesNewVersionWithoutRewritingOldFields() {
    var created = service.create(1L, create("Health"));
    var first = service.templates(1L, created.id()).get(0);
    String stableKey = first.fields().get(0).key();
    var second = service.publishTemplate(1L, created.id(), new NotebookTemplateRequest(List.of(
        new NotebookFieldInput(stableKey, "TEXT", "Updated label", true, List.of(), null))));
    assertThat(second.version()).isEqualTo(2);
    assertThat(second.fields().get(0).key()).isEqualTo(stableKey);
    var third = service.publishTemplate(1L, created.id(), new NotebookTemplateRequest(List.of(
        new NotebookFieldInput(stableKey, "NUMBER", "Updated label", true, List.of(), "kg"))));
    assertThat(third.fields().get(0).key()).isNotEqualTo(stableKey);
    assertThat(service.templates(1L, created.id())).extracting(template -> template.version())
        .containsExactly(1, 2, 3);
    assertThat(service.templates(1L, created.id()).get(0).fields().get(0).label()).isEqualTo("Text");
  }

  @Test void deletionRequiresConfirmationAndKeepsMetadataAudit() {
    var created = service.create(1L, create("Delete me"));
    var template = service.templates(1L, created.id()).get(0);
    sql.update("INSERT INTO notebook_records(event_id,owner_user_id,created_by_user_id,"
        + "updated_by_user_id,occurred_from,occurred_to,title,template_version,values_json) "
        + "VALUES (?,?,?,?,?,?,?,?,?)", created.id(), 1, 1, 1, "2026-01-01", "2026-01-01",
        "Record", template.version(), "{}");
    sql.update("INSERT INTO notebook_grants(event_id,owner_user_id,grantee_user_id,data_from,data_to,"
        + "valid_from,valid_to) VALUES (?,?,?,'2026-01-01','2026-01-31','2026-01-01','2026-02-01')",
        created.id(), 1, 2);
    var impact = service.deleteImpact(1L, created.id());
    assertThat(impact.recordCount()).isEqualTo(1);
    assertThat(impact.templateVersionCount()).isEqualTo(1);
    assertThat(impact.grantCount()).isEqualTo(1);
    assertThatThrownBy(() -> service.delete(1L, created.id(), false)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.delete(2L, created.id(), true)).isInstanceOf(BusinessException.class);
    service.delete(1L, created.id(), true);
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM notebook_events WHERE id=?", Integer.class, created.id()))
        .isZero();
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM notebook_records WHERE event_id=?", Integer.class, created.id()))
        .isZero();
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM notebook_audit WHERE event_id=? AND action='EVENT_DELETE'",
        Integer.class, created.id())).isEqualTo(1);
  }

  @Test void doesNotDeleteFilesWhenPrivateRootOverlapsPublicUploads() throws Exception {
    Path root = testRoot();
    var created = service.create(1L, create("Protected storage"));
    sql.update("INSERT INTO notebook_records(event_id,owner_user_id,created_by_user_id,"
        + "updated_by_user_id,occurred_from,occurred_to,title,template_version,values_json) "
        + "VALUES (?,?,?,?,?,?,?,?,?)", created.id(), 1, 1, 1, "2026-01-01", "2026-01-01",
        "Record", 1, "{}");
    long recordId = sql.queryForObject("SELECT id FROM notebook_records WHERE event_id=?", Long.class, created.id());
    String key = java.util.UUID.randomUUID() + ".png";
    Path image = root.resolve(key);
    Files.write(image, new byte[] {1, 2, 3});
    sql.update("INSERT INTO notebook_images(record_id,owner_user_id,storage_key,original_name,"
        + "content_type,byte_size) VALUES (?,?,?,?,?,?)", recordId, 1, key, "image.png", "image/png", 3);
    ReflectionTestUtils.setField(service, "privateRoot", root.toString());
    ReflectionTestUtils.setField(service, "publicRoot", root.toString());

    service.delete(1L, created.id(), true);

    assertThat(Files.exists(image)).isTrue();
  }

  @Test void confirmedDeletionCleansBoundedPrivateImageFiles() throws Exception {
    Path root = testRoot();
    Path privateRoot = Files.createDirectory(root.resolve("private"));
    Path publicRoot = Files.createDirectory(root.resolve("public"));
    var created = service.create(1L, create("Private image"));
    sql.update("INSERT INTO notebook_records(event_id,owner_user_id,created_by_user_id,"
        + "updated_by_user_id,occurred_from,occurred_to,title,template_version,values_json) "
        + "VALUES (?,?,?,?,?,?,?,?,?)", created.id(), 1, 1, 1, "2026-01-01", "2026-01-01",
        "Record", 1, "{}");
    long recordId = sql.queryForObject("SELECT id FROM notebook_records WHERE event_id=?", Long.class, created.id());
    String key = java.util.UUID.randomUUID() + ".png";
    Path image = privateRoot.resolve(key);
    Files.write(image, new byte[] {1, 2, 3});
    sql.update("INSERT INTO notebook_images(record_id,owner_user_id,storage_key,original_name,"
        + "content_type,byte_size) VALUES (?,?,?,?,?,?)", recordId, 1, key, "image.png", "image/png", 3);
    ReflectionTestUtils.setField(service, "privateRoot", privateRoot.toString());
    ReflectionTestUtils.setField(service, "publicRoot", publicRoot.toString());

    service.delete(1L, created.id(), true);

    assertThat(Files.exists(image)).isFalse();
  }

  private static NotebookEventCreate create(String name) {
    return new NotebookEventCreate(name, "Personal", "Description", List.of(
        new NotebookFieldInput(null, "TEXT", "Text", true, List.of(), null)));
  }

  private Path testRoot() throws Exception {
    Path target = Path.of("target").toAbsolutePath().normalize();
    cleanupRoot = Files.createTempDirectory(target, "notebook-cleanup-");
    return cleanupRoot;
  }
}
