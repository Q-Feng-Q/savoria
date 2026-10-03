package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.model.NotebookEventCreate;
import com.familykitchen.notebook.model.NotebookFieldInput;
import com.familykitchen.notebook.model.NotebookRecordCreate;
import com.familykitchen.notebook.model.NotebookRecordPatch;
import com.familykitchen.notebook.model.NotebookTemplateRequest;
import com.familykitchen.notebook.service.NotebookEventService;
import com.familykitchen.notebook.service.NotebookRecordService;
import com.familykitchen.notebook.service.NotebookRecordValidator;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookTemplateValidator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

/** Record snapshots, ownership and optimistic-write integration tests. */
class NotebookRecordServiceTest {
  private NotebookEventService events;
  private NotebookRecordService records;
  private JdbcTemplate sql;
  private NotebookRecordMapper mapper;
  private DataSourceTransactionManager transactions;

  @BeforeEach void database() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:record_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
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
    sql.update("INSERT INTO users(id) VALUES (1),(2)");
    transactions = new DataSourceTransactionManager(source);
    var eventMapper = new NotebookEventMapper(sql);
    var json = new ObjectMapper();
    events = new NotebookEventService(eventMapper, new NotebookTemplateValidator(), json, transactions);
    mapper = spy(new NotebookRecordMapper(sql));
    records = new NotebookRecordService(mapper, eventMapper,
        new NotebookRecordValidator(), new NotebookRangePolicy(() -> 36), json,
        new NotebookAccessPolicy(eventMapper, new NotebookContactMapper(sql), new NotebookGrantMapper(sql)));
  }

  @Test void createsSingleAndCrossDayAndRejectsReversedOrForeignEvent() {
    long event = event();
    var single = records.create(1, event, create("Single", "2026-01-01T10:00:00+08:00",
        "2026-01-01T10:00:00+08:00"));
    var cross = records.create(1, event, create("Cross", "2026-01-31T10:00:00+08:00",
        "2026-02-02T10:00:00+08:00"));
    assertThat(single.templateVersion()).isEqualTo(1);
    assertThat(cross.occurredTo().toLocalDate()).isEqualTo(LocalDate.of(2026, 2, 2));
    assertThatThrownBy(() -> records.create(2, event, create("Foreign", "2026-01-01T10:00:00+08:00",
        "2026-01-01T10:00:00+08:00"))).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> records.create(1, event, create("Reverse", "2026-02-01T10:00:00+08:00",
        "2026-01-01T10:00:00+08:00"))).isInstanceOf(BusinessException.class);
  }

  @Test void oldVersionRemainsEditableAndExplicitUpgradePreservesPriorRevision() {
    long event = event();
    var first = records.create(1, event, create("Old", "2026-01-01T10:00:00+08:00",
        "2026-01-01T10:00:00+08:00"));
    String key = events.templates(1, event).get(0).fields().get(0).key();
    events.publishTemplate(1, event, new NotebookTemplateRequest(List.of(
        new NotebookFieldInput(key, "TEXT", "Renamed", true, List.of(), null),
        new NotebookFieldInput(null, "BOOLEAN", "Check", false, List.of(), null))));
    var edited = records.update(1, first.id(), new NotebookRecordPatch(first.lockVersion(), "Edited", null,
        null, null, Map.of(key, "old value")));
    assertThat(edited.templateVersion()).isEqualTo(1);
    assertThat(edited.fields().get(0).label()).isEqualTo("Text");
    var upgraded = records.upgradeTemplate(1, first.id(), edited.lockVersion(), Map.of(key, "new value"));
    assertThat(upgraded.templateVersion()).isEqualTo(2);
    assertThat(upgraded.fields()).hasSize(2);
    assertThat(sql.queryForObject("SELECT template_version FROM notebook_record_revisions WHERE record_id=?",
        Integer.class, first.id())).isEqualTo(1);
    assertThatThrownBy(() -> records.update(1, first.id(), new NotebookRecordPatch(first.lockVersion(),
        "stale", null, null, null, Map.of(key, "stale"))))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.STATE_CONFLICT));
  }

  @Test void ownerCanDeleteButOtherAccountCannotReadOrDelete() {
    long event = event();
    var record = records.create(1, event, create("Private", "2026-01-01T10:00:00+08:00",
        "2026-01-01T10:00:00+08:00"));
    assertThatThrownBy(() -> records.get(2, record.id())).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> records.delete(2, record.id())).isInstanceOf(BusinessException.class);
    records.delete(1, record.id());
    assertThatThrownBy(() -> records.get(1, record.id())).isInstanceOf(BusinessException.class);
  }

  @Test void staleUpgradeAfterConcurrentWinnerReturnsConflictAndKeepsOneRevision() {
    long event = event();
    var record = records.create(1, event, create("Race", "2026-01-01T10:00:00+08:00",
        "2026-01-01T10:00:00+08:00"));
    String key = events.templates(1, event).get(0).fields().get(0).key();
    events.publishTemplate(1, event, new NotebookTemplateRequest(List.of(
        new NotebookFieldInput(key, "TEXT", "Revised", true, List.of(), null))));
    var staleRead = mapper.find(1, record.id());
    var factory = new ProxyFactory(records);
    factory.setProxyTargetClass(true);
    factory.addAdvice(new TransactionInterceptor(transactions,
        new AnnotationTransactionAttributeSource()));
    var proxied = (NotebookRecordService) factory.getProxy();
    proxied.upgradeTemplate(1, record.id(), record.lockVersion(), Map.of(key, "winner"));

    // Model a loser that read the old row before the winner committed.
    doReturn(staleRead).when(mapper).findAny(record.id());
    assertThatThrownBy(() -> proxied.upgradeTemplate(1, record.id(), record.lockVersion(),
        Map.of(key, "loser")))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.STATE_CONFLICT));
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM notebook_record_revisions WHERE record_id=?",
        Integer.class, record.id())).isEqualTo(1);
    assertThat(sql.queryForObject("SELECT values_json FROM notebook_records WHERE id=?", String.class,
        record.id())).contains("winner");
  }

  @Test void queriesOverlapInDeterministicPagesAndProjectsCalendarWithoutValues() {
    long event = event();
    records.create(1, event, create("Later", "2026-02-02T10:00:00+00:00",
        "2026-02-02T10:00:00+00:00"));
    records.create(1, event, create("Cross", "2026-01-31T10:00:00+00:00",
        "2026-02-03T10:00:00+00:00"));
    records.create(1, event, create("Early", "2026-01-01T10:00:00+00:00",
        "2026-01-01T10:00:00+00:00"));
    var first = records.list(1, event, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
        "UTC", 0, 1);
    assertThat(first.items()).extracting(item -> item.title()).containsExactly("Cross");
    assertThat(first.hasMore()).isTrue();
    assertThat(records.list(1, event, LocalDate.of(2026, 2, 1),
        LocalDate.of(2026, 2, 28), "UTC", 1, 1).items()).extracting(item -> item.title())
        .containsExactly("Later");
    assertThat(records.calendar(1, event, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 3), "UTC"))
        .extracting(item -> item.recordCount()).containsExactly(1L, 2L, 1L);
    assertThatThrownBy(() -> records.list(2, event, LocalDate.of(2026, 2, 1),
        LocalDate.of(2026, 2, 28), "UTC", 0, 10)).isInstanceOf(BusinessException.class);
  }

  @Test void permitsThirtySixTouchedMonthsButRejectsThirtySevenAndUnboundedPages() {
    long event = event();
    assertThat(records.list(1, event, LocalDate.of(2024, 1, 1),
        LocalDate.of(2026, 12, 31), "UTC", 0, 10).items()).isEmpty();
    assertThatThrownBy(() -> records.list(1, event, LocalDate.of(2024, 1, 1),
        LocalDate.of(2027, 1, 1), "UTC", 0, 10)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> records.list(1, event, null, null, "UTC", 0, 10))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> records.list(1, event, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31), "UTC", 0, 101)).isInstanceOf(BusinessException.class);
  }

  @Test void localMidnightUsesRequestedIanaZoneAndRejectsInvalidZone() {
    long event = event();
    records.create(1, event, create("Shanghai New Year", "2026-01-01T00:30:00+08:00",
        "2026-01-01T01:00:00+08:00"));
    assertThat(records.list(1, event, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
        "Asia/Shanghai", 0, 10).items()).extracting(item -> item.title())
        .containsExactly("Shanghai New Year");
    assertThat(records.calendar(1, event, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
        "Asia/Shanghai")).extracting(item -> item.date()).containsExactly(LocalDate.of(2026, 1, 1));
    assertThat(records.list(1, event, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
        "UTC", 0, 10).items()).isEmpty();
    assertThatThrownBy(() -> records.calendar(1, event, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 1), "Invalid/Zone"))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
  }

  @Test void calendarCountsMoreThanOneProjectionBatch() {
    long event = event();
    for (int index = 0; index < 205; index++) {
      records.create(1, event, create("Many " + index, "2026-01-01T12:00:00+00:00",
          "2026-01-01T12:00:00+00:00"));
    }
    assertThat(records.calendar(1, event, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
        "UTC")).extracting(item -> item.recordCount()).containsExactly(205L);
  }

  @Test void deletionQueuesPrivateImageBeforeCascadingItsMetadata() {
    long event = event();
    var record = records.create(1, event, create("With image", "2026-01-01T10:00:00+00:00",
        "2026-01-01T10:00:00+00:00"));
    String key = "12e45678-e89b-12d3-a456-426614174000.png";
    sql.update("INSERT INTO notebook_images(record_id,owner_user_id,storage_key,original_name,"
        + "content_type,byte_size) VALUES (?,?,?,?,?,?)", record.id(), 1, key, "photo.png", "image/png", 4);
    records.delete(1, record.id());
    assertThat(sql.queryForObject("SELECT storage_key FROM notebook_image_cleanup WHERE event_id=?",
        String.class, event)).isEqualTo(key);
    assertThat(sql.queryForObject("SELECT COUNT(*) FROM notebook_images WHERE record_id=?",
        Integer.class, record.id())).isZero();
  }

  private long event() {
    return events.create(1, new NotebookEventCreate("Journal", null, null,
        List.of(new NotebookFieldInput(null, "TEXT", "Text", true, List.of(), null)))).id();
  }

  private NotebookRecordCreate create(String title, String from, String to) {
    String key = events.templates(1, events.list(1, true).get(0).id()).get(0).fields().get(0).key();
    return new NotebookRecordCreate(OffsetDateTime.parse(from), OffsetDateTime.parse(to), title,
        null, Map.of(key, "value"));
  }
}
