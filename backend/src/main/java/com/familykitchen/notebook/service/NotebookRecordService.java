package com.familykitchen.notebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.NotebookRangePolicy;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import com.familykitchen.notebook.model.NotebookCalendarSummary;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.model.NotebookRecordCreate;
import com.familykitchen.notebook.model.NotebookRecordPage;
import com.familykitchen.notebook.model.NotebookRecordPatch;
import com.familykitchen.notebook.model.NotebookRecordView;
import com.familykitchen.system.service.SystemSettingService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account-owned record lifecycle with immutable template snapshots. */
@Service
public class NotebookRecordService {
  private static final int CALENDAR_BATCH_SIZE = 200;
  private final NotebookRecordMapper mapper;
  private final NotebookEventMapper events;
  private final NotebookRecordValidator validator;
  private final NotebookRangePolicy ranges;
  private final ObjectMapper json;

  /** Creates the record service using the live global month setting.
   * @param mapper record persistence
   * @param events event/template persistence
   * @param validator field-value validator
   * @param settings global setting service
   * @param json JSON codec */
  @Autowired
  public NotebookRecordService(NotebookRecordMapper mapper, NotebookEventMapper events,
      NotebookRecordValidator validator, SystemSettingService settings, ObjectMapper json) {
    this(mapper, events, validator, new NotebookRangePolicy(settings::notebookMaxQueryMonths), json);
  }

  /** Creates a record service with an explicit range policy for isolated integrations.
   * @param mapper record persistence
   * @param events event/template persistence
   * @param validator field-value validator
   * @param ranges date-range policy
   * @param json JSON codec */
  public NotebookRecordService(NotebookRecordMapper mapper, NotebookEventMapper events,
      NotebookRecordValidator validator, NotebookRangePolicy ranges, ObjectMapper json) {
    this.mapper = mapper; this.events = events; this.validator = validator;
    this.ranges = ranges; this.json = json;
  }

  /** Creates a record bound to the current template.
   * @param actor authenticated owner account
   * @param eventId account-owned event
   * @param request record draft
   * @return created record */
  @Transactional
  public NotebookRecordView create(long actor, long eventId, NotebookRecordCreate request) {
    var event = requireEvent(actor, eventId);
    if (request == null) throw bad("Record body is required");
    checkInterval(request.occurredFrom(), request.occurredTo());
    String title = title(request.title());
    String note = note(request.note());
    var fields = fields(eventId, event.currentTemplateVersion());
    validator.validate(fields, request.values());
    long id = mapper.insert(eventId, event.ownerUserId(), actor, request.occurredFrom().toInstant(),
        request.occurredTo().toInstant(), title, note, event.currentTemplateVersion(),
        encode(request.values()));
    mapper.audit(actor, event.ownerUserId(), eventId, id, "RECORD_CREATE");
    return get(actor, id);
  }

  /** Reads a record and its original field definitions for its owner.
   * @param actor authenticated owner account
   * @param recordId record ID
   * @return record with bound template fields */
  public NotebookRecordView get(long actor, long recordId) {
    return view(requireRecord(actor, recordId));
  }

  /** Edits a record while retaining its original template version.
   * @param actor authenticated owner account
   * @param recordId record ID
   * @param patch version-checked edit
   * @return updated record */
  @Transactional
  public NotebookRecordView update(long actor, long recordId, NotebookRecordPatch patch) {
    var old = requireRecord(actor, recordId);
    if (patch == null || patch.expectedVersion() == null) throw bad("Expected record version is required");
    if (patch.expectedVersion() != old.lockVersion()) throw conflict();
    OffsetDateTime from = patch.occurredFrom() == null ? utc(old.from()) : patch.occurredFrom();
    OffsetDateTime to = patch.occurredTo() == null ? utc(old.to()) : patch.occurredTo();
    checkInterval(from, to);
    Map<String, Object> values = patch.values() == null ? decodeValues(old.valuesJson()) : patch.values();
    validator.validate(fields(old.eventId(), old.templateVersion()), values);
    var desired = new NotebookRecordMapper.Row(old.id(), old.eventId(), old.ownerUserId(),
        old.createdByUserId(), actor, from.toInstant(), to.toInstant(),
        patch.title() == null ? old.title() : title(patch.title()),
        patch.note() == null ? old.note() : note(patch.note()), old.templateVersion(),
        encode(values), old.lockVersion());
    if (mapper.update(desired, patch.expectedVersion()) != 1) throw conflict();
    mapper.audit(actor, old.ownerUserId(), old.eventId(), recordId, "RECORD_UPDATE");
    return get(actor, recordId);
  }

  /** Explicitly upgrades one record to the event's current template and snapshots its prior state.
   * @param actor authenticated owner account
   * @param recordId record ID
   * @param expectedVersion last observed lock version
   * @param values complete values for the new template
   * @return upgraded record */
  @Transactional
  public NotebookRecordView upgradeTemplate(long actor, long recordId, Integer expectedVersion,
      Map<String, Object> values) {
    var old = requireRecord(actor, recordId);
    if (expectedVersion == null) throw bad("Expected record version is required");
    if (expectedVersion != old.lockVersion()) throw conflict();
    var event = requireEvent(actor, old.eventId());
    if (event.currentTemplateVersion() == old.templateVersion()) {
      throw bad("Record already uses the current template");
    }
    validator.validate(fields(old.eventId(), event.currentTemplateVersion()), values);
    var desired = new NotebookRecordMapper.Row(old.id(), old.eventId(), old.ownerUserId(),
        old.createdByUserId(), actor, old.from(), old.to(), old.title(), old.note(),
        event.currentTemplateVersion(), encode(values), old.lockVersion());
    if (mapper.update(desired, expectedVersion) != 1) throw conflict();
    mapper.revision(old, actor);
    mapper.audit(actor, old.ownerUserId(), old.eventId(), recordId, "RECORD_TEMPLATE_UPGRADE");
    return get(actor, recordId);
  }

  /** Deletes an owner record and records a content-free audit entry.
   * @param actor authenticated owner account
   * @param recordId record ID */
  @Transactional
  public void delete(long actor, long recordId) {
    var old = requireRecord(actor, recordId);
    long lastImageId = 0;
    while (true) {
      var batch = mapper.imageKeysAfter(recordId, lastImageId, 100);
      if (batch.isEmpty()) break;
      events.enqueueImageCleanup(old.eventId(), old.ownerUserId(), batch);
      lastImageId = batch.get(batch.size() - 1).id();
      if (batch.size() < 100) break;
    }
    if (mapper.delete(actor, recordId) != 1) throw missing();
    mapper.audit(actor, old.ownerUserId(), old.eventId(), recordId, "RECORD_DELETE");
  }

  /** Returns a bounded and deterministic owner record page.
   * @param actor authenticated owner account
   * @param eventId event ID
   * @param from inclusive date
   * @param to inclusive date
   * @param timeZone IANA time-zone ID defining calendar days
   * @param page zero-based page
   * @param size page size
   * @return matching records */
  public NotebookRecordPage list(long actor, long eventId, LocalDate from, LocalDate to,
      String timeZone, int page, int size) {
    requireEvent(actor, eventId);
    ranges.requireAllowed(from, to);
    ZoneId zone = zone(timeZone);
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
      throw bad("Invalid record page");
    }
    var rows = mapper.page(actor, eventId, dayStart(from, zone), dayEndExclusive(to, zone),
        size + 1, (long) page * size);
    boolean hasMore = rows.size() > size;
    return new NotebookRecordPage(rows.stream().limit(size).map(this::view).toList(), page, size, hasMore);
  }

  /** Projects date counts without loading values or template fields.
   * @param actor authenticated owner account
   * @param eventId event ID
   * @param from inclusive date
   * @param to inclusive date
   * @param timeZone IANA time-zone ID defining calendar days
   * @return ordered nonempty calendar days */
  public List<NotebookCalendarSummary> calendar(long actor, long eventId, LocalDate from, LocalDate to,
      String timeZone) {
    requireEvent(actor, eventId);
    ranges.requireAllowed(from, to);
    ZoneId zone = zone(timeZone);
    Map<LocalDate, Long> counts = new TreeMap<>();
    long cursor = 0;
    while (true) {
      var batch = mapper.intervalsAfter(actor, eventId, dayStart(from, zone),
          dayEndExclusive(to, zone), cursor, CALENDAR_BATCH_SIZE);
      if (batch.isEmpty()) break;
      for (var interval : batch) {
        LocalDate first = interval.from().atZone(zone).toLocalDate();
        LocalDate last = interval.to().atZone(zone).toLocalDate();
        for (LocalDate day = first.isBefore(from) ? from : first;
            !day.isAfter(last) && !day.isAfter(to); day = day.plusDays(1)) {
          counts.merge(day, 1L, Long::sum);
        }
      }
      cursor = batch.get(batch.size() - 1).id();
      if (batch.size() < CALENDAR_BATCH_SIZE) break;
    }
    List<NotebookCalendarSummary> result = new ArrayList<>();
    counts.forEach((date, count) -> result.add(new NotebookCalendarSummary(date, count)));
    return List.copyOf(result);
  }

  private com.familykitchen.notebook.model.NotebookEventView requireEvent(long actor, long eventId) {
    var event = events.findEvent(eventId);
    if (event == null || event.ownerUserId() != actor) throw missing();
    return event;
  }

  private NotebookRecordMapper.Row requireRecord(long actor, long recordId) {
    var row = mapper.find(actor, recordId);
    if (row == null) throw missing();
    return row;
  }

  private NotebookRecordView view(NotebookRecordMapper.Row row) {
    return new NotebookRecordView(row.id(), row.eventId(), row.ownerUserId(), row.createdByUserId(),
        row.updatedByUserId(), utc(row.from()), utc(row.to()), row.title(), row.note(),
        row.templateVersion(), decodeValues(row.valuesJson()), fields(row.eventId(), row.templateVersion()),
        row.lockVersion());
  }

  private List<NotebookField> fields(long eventId, int version) {
    var template = events.template(eventId, version);
    if (template == null) throw new IllegalStateException("Notebook record template is missing");
    try { return json.readValue(template.fieldsJson(), new TypeReference<List<NotebookField>>() {}); }
    catch (JsonProcessingException failure) { throw new IllegalStateException("Invalid notebook template JSON", failure); }
  }

  private Map<String, Object> decodeValues(String value) {
    try { return json.readValue(value, new TypeReference<Map<String, Object>>() {}); }
    catch (JsonProcessingException failure) { throw new IllegalStateException("Invalid notebook record JSON", failure); }
  }

  private String encode(Map<String, Object> values) {
    try { return json.writeValueAsString(values); }
    catch (JsonProcessingException failure) { throw bad("Invalid notebook record values"); }
  }

  private static void checkInterval(OffsetDateTime from, OffsetDateTime to) {
    if (from == null || to == null || from.toInstant().isAfter(to.toInstant())) {
      throw bad("Invalid notebook occurrence interval");
    }
  }

  private static String title(String value) {
    if (value == null || value.isBlank() || value.strip().length() > 200) throw bad("Invalid record title");
    return value.strip();
  }

  private static String note(String value) {
    if (value == null || value.isBlank()) return null;
    if (value.length() > 20000) throw bad("Record note is too long");
    return value;
  }

  private static ZoneId zone(String value) {
    if (value == null || !ZoneId.getAvailableZoneIds().contains(value)) {
      throw bad("A valid IANA notebook timeZone is required");
    }
    return ZoneId.of(value);
  }

  private static Instant dayStart(LocalDate day, ZoneId zone) {
    return day.atStartOfDay(zone).toInstant();
  }

  private static Instant dayEndExclusive(LocalDate day, ZoneId zone) {
    if (day.equals(LocalDate.MAX)) throw bad("Invalid notebook date range");
    return dayStart(day.plusDays(1), zone);
  }
  private static OffsetDateTime utc(Instant value) { return value.atOffset(ZoneOffset.UTC); }
  private static BusinessException bad(String message) { return new BusinessException(ErrorCode.BAD_REQUEST, message); }
  private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "Notebook record not found"); }
  private static BusinessException conflict() { return new BusinessException(ErrorCode.STATE_CONFLICT, "Notebook record changed; reload it"); }
}
