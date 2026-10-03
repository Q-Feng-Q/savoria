package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import com.familykitchen.notebook.model.NotebookRecordCreate;
import com.familykitchen.notebook.model.NotebookRecordPatch;
import com.familykitchen.notebook.model.NotebookGrantRequest;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookGrantService;
import com.familykitchen.notebook.service.NotebookRecordService;
import com.familykitchen.notebook.service.NotebookRecordValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Independent grant capabilities and full interval containment. */
class NotebookGrantPolicyTest {
  private NotebookGrantService grants;
  private NotebookAccessPolicy policy;
  private JdbcTemplate sql;
  private NotebookRecordService records;

  @BeforeEach void database() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:grants_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    sql = new JdbcTemplate(source);
    sql.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
    sql.execute("CREATE TABLE system_settings (id BIGINT PRIMARY KEY)");
    for (String migration : List.of("V5__personal_notebook.sql", "V6__notebook_image_cleanup_queue.sql",
        "V7__notebook_grant_data_zone.sql")) {
      try (var stream = getClass().getResourceAsStream("/db/migration/" + migration)) {
        for (String command : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
          if (!command.isBlank()) sql.execute(command);
        }
      }
    }
    sql.update("INSERT INTO users(id) VALUES (1),(2),(3)");
    sql.update("INSERT INTO notebook_events(owner_user_id,name) VALUES (1,'Private')");
    var contacts = new NotebookContactMapper(sql);
    var mapper = new NotebookGrantMapper(sql);
    var events = new NotebookEventMapper(sql);
    policy = new NotebookAccessPolicy(events, contacts, mapper);
    grants = new NotebookGrantService(events, contacts, mapper, new NotebookRangePolicy(() -> 36));
    records = new NotebookRecordService(new NotebookRecordMapper(sql), events,
        new NotebookRecordValidator(), new NotebookRangePolicy(() -> 36), new ObjectMapper(), policy);
  }

  @Test void grantDefaultsPrivateAndRequiresConfirmedContact() {
    assertThat(policy.shared(2)).isEmpty();
    assertThatThrownBy(() -> grants.create(1, 1, request(false, false, false)))
        .isInstanceOf(BusinessException.class);
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (1,2,'LOGIN'),(2,1,'LOGIN')");
    var grant = grants.create(1, 1, request(false, false, false));
    assertThat(grant.canCreate()).isFalse();
    assertThat(grant.canEdit()).isFalse();
    assertThat(grant.canExport()).isFalse();
    assertThat(policy.shared(2)).hasSize(1);
    assertThatThrownBy(() -> policy.requireCreate(2, 1, instant("2026-01-10T12:00:00+08:00"),
        instant("2026-01-10T13:00:00+08:00"))).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> grants.create(2, 1, request(true, true, true)))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test void independentCapabilitiesAndFullLocalContainment() {
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (1,2,'LOGIN'),(2,1,'LOGIN')");
    grants.create(1, 1, request(true, false, true));
    assertThat(policy.requireCreate(2, 1, instant("2026-01-01T00:30:00+08:00"),
        instant("2026-01-01T01:00:00+08:00")).ownerUserId()).isEqualTo(1);
    assertThatThrownBy(() -> policy.requireCreate(2, 1, instant("2025-12-31T23:59:00+08:00"),
        instant("2026-01-01T01:00:00+08:00"))).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> policy.requireEdit(2, 1, instant("2026-01-10T12:00:00+08:00"),
        instant("2026-01-10T13:00:00+08:00"))).isInstanceOf(BusinessException.class);
    assertThat(policy.requireExport(2, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
        "Asia/Shanghai").ownerUserId()).isEqualTo(1);
    assertThatThrownBy(() -> policy.requireRead(2, 1, LocalDate.of(2026, 2, 1),
        LocalDate.of(2026, 2, 28), "Asia/Shanghai"))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test void rangeLimitAndValidityAreDistinctAndRevocationImmediate() {
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (1,2,'LOGIN'),(2,1,'LOGIN')");
    var valid36 = new NotebookGrantRequest(2, LocalDate.of(2024, 1, 1),
        LocalDate.of(2026, 12, 31), "Asia/Shanghai", OffsetDateTime.parse("2025-01-01T00:00:00Z"),
        OffsetDateTime.parse("2030-01-01T00:00:00Z"), true, true, false);
    var grant = grants.create(1, 1, valid36);
    assertThatThrownBy(() -> grants.update(1, grant.id(), new NotebookGrantRequest(2,
        LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1), "Asia/Shanghai",
        valid36.validFrom(), valid36.validTo(), true, true, false)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> grants.update(1, grant.id(), new NotebookGrantRequest(2,
        valid36.dataFrom(), valid36.dataTo(), "Asia/Shanghai", valid36.validTo(),
        valid36.validFrom(), true, true, false))).isInstanceOf(BusinessException.class);
    grants.revoke(1, grant.id());
    assertThatThrownBy(() -> policy.requireRead(2, 1, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31), "Asia/Shanghai"))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test void expiredGrantIsInvisibleAndNeverConfersManagement() {
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (1,2,'LOGIN'),(2,1,'LOGIN')");
    var grant = grants.create(1, 1, request(true, true, true));
    sql.update("UPDATE notebook_grants SET valid_to='2000-01-01' WHERE id=?", grant.id());
    assertThat(policy.shared(2)).isEmpty();
    assertThatThrownBy(() -> policy.requireRead(2, 1, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31), "Asia/Shanghai")).isInstanceOf(BusinessException.class);
  }

  @Test void sharedEditorCanEditOwnersRecordButCannotDeleteOrSeeBoundaryCrossingRecord() {
    sql.update("INSERT INTO notebook_template_versions(event_id,version,fields_json,published_by_user_id) "
        + "VALUES (1,1,'[]',1)");
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (1,2,'LOGIN'),(2,1,'LOGIN')");
    grants.create(1, 1, request(true, true, false));
    var ownerRecord = records.create(1, 1, new NotebookRecordCreate(
        instant("2026-01-01T00:30:00+08:00"), instant("2026-01-01T01:00:00+08:00"),
        "Owner", null, java.util.Map.of()));
    var boundaryRecord = records.create(1, 1, new NotebookRecordCreate(
        instant("2026-01-31T23:00:00+08:00"), instant("2026-02-01T01:00:00+08:00"),
        "Boundary", null, java.util.Map.of()));
    var edited = records.update(2, ownerRecord.id(), new NotebookRecordPatch(
        ownerRecord.lockVersion(), "Shared edit", null, null, null, java.util.Map.of()));
    assertThat(edited.ownerUserId()).isEqualTo(1);
    assertThat(edited.createdByUserId()).isEqualTo(1);
    assertThat(edited.updatedByUserId()).isEqualTo(2);
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (1,3,'LOGIN'),(3,1,'LOGIN')");
    grants.create(1, 1, new NotebookGrantRequest(3, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31), "Asia/Shanghai",
        OffsetDateTime.parse("2025-01-01T00:00:00Z"),
        OffsetDateTime.parse("2030-01-01T00:00:00Z"), true, false, false));
    var collaboratorRecord = records.create(3, 1, new NotebookRecordCreate(
        instant("2026-01-02T10:00:00+08:00"), instant("2026-01-02T11:00:00+08:00"),
        "Collaborator", null, java.util.Map.of()));
    var collaborationEdit = records.update(2, collaboratorRecord.id(), new NotebookRecordPatch(
        collaboratorRecord.lockVersion(), "Edited by another collaborator", null, null, null,
        java.util.Map.of()));
    assertThat(collaborationEdit.createdByUserId()).isEqualTo(3);
    assertThat(collaborationEdit.updatedByUserId()).isEqualTo(2);
    assertThat(records.list(2, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
        "Asia/Shanghai", 0, 20).items()).extracting(item -> item.title())
        .containsExactly("Shared edit", "Edited by another collaborator");
    assertThatThrownBy(() -> records.get(2, boundaryRecord.id())).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> records.delete(2, ownerRecord.id())).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> records.upgradeTemplate(2, ownerRecord.id(), edited.lockVersion(),
        java.util.Map.of())).isInstanceOf(BusinessException.class);
  }

  private static NotebookGrantRequest request(boolean create, boolean edit, boolean export) {
    return new NotebookGrantRequest(2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
        "Asia/Shanghai", OffsetDateTime.parse("2025-01-01T00:00:00Z"),
        OffsetDateTime.parse("2030-01-01T00:00:00Z"), create, edit, export);
  }

  private static OffsetDateTime instant(String text) { return OffsetDateTime.parse(text); }
}
