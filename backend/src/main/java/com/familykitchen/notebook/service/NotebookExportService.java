package com.familykitchen.notebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.NotebookRangePolicy;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.model.NotebookRecordView;
import com.familykitchen.notebook.model.NotebookTemplateView;
import com.familykitchen.system.service.SystemSettingService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Builds a private v1 JSON export after explicit and late permission checks. */
@Service
public class NotebookExportService {
  private final NotebookAccessPolicy access;
  private final NotebookEventMapper events;
  private final NotebookRecordService records;
  private final NotebookImageService images;
  private final NotebookAuditService audit;
  private final NotebookRangePolicy ranges;
  private final ObjectMapper json;

  /** Creates export operations using the live system month limit.
   *
   * @param access account policy
   * @param events event persistence
   * @param records record service
   *
   * @param images image metadata
   * @param audit action history
   * @param settings global settings
   *
   * @param json JSON codec */
  @Autowired
  public NotebookExportService(NotebookAccessPolicy access, NotebookEventMapper events,
      NotebookRecordService records, NotebookImageService images, NotebookAuditService audit,
      SystemSettingService settings, ObjectMapper json) {
    this(access, events, records, images, audit,
        new NotebookRangePolicy(settings::notebookMaxQueryMonths), json);
  }

  /** Creates export operations with explicit range policy for isolated tests.
   *
   * @param access account policy
   * @param events event persistence
   * @param records record service
   *
   * @param images image metadata
   * @param audit action history
   * @param ranges date policy
   *
   * @param json JSON codec */
  public NotebookExportService(NotebookAccessPolicy access, NotebookEventMapper events,
      NotebookRecordService records, NotebookImageService images, NotebookAuditService audit,
      NotebookRangePolicy ranges, ObjectMapper json) {
    this.access = access; this.events = events; this.records = records; this.images = images;
    this.audit = audit; this.ranges = ranges; this.json = json;
  }

  /** Returns one JSON-ready payload for clipboard or file workflows.
   *
   * @param actor authenticated user
   * @param eventId event ID
   * @param from inclusive local date
   *
   * @param to inclusive local date
   * @param timeZone IANA calendar zone
   *
   * @param mode COPY or FILE
   * @return complete export payload */
  public Map<String, Object> export(long actor, long eventId, LocalDate from, LocalDate to,
      String timeZone, String mode) {
    ranges.requireAllowed(from, to);
    if (timeZone == null || !ZoneId.getAvailableZoneIds().contains(timeZone)
        || !List.of("COPY", "FILE").contains(mode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid notebook export request");
    }
    var scope = access.requireExport(actor, eventId, from, to, timeZone);
    var event = events.findEvent(eventId);
    if (event == null || event.ownerUserId() != scope.ownerUserId()) throw missing();
    List<NotebookRecordView> selected = new ArrayList<>();
    for (int page = 0; ; page++) {
      var batch = records.list(actor, eventId, from, to, timeZone, page, 100);
      selected.addAll(batch.items());
      if (!batch.hasMore()) break;
      if (page == Integer.MAX_VALUE) throw new IllegalStateException("Notebook export page overflow");
    }
    Map<Integer, NotebookTemplateView> templates = new TreeMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (var record : selected) {
      if (!scope.contains(record.occurredFrom().toInstant(), record.occurredTo().toInstant())) continue;
      int version = record.templateVersion();
      if (!templates.containsKey(version)) {
        var immutable = events.template(eventId, version);
        if (immutable == null) throw new IllegalStateException("Export template missing");
        templates.put(version, new NotebookTemplateView(eventId, version, fields(immutable.fieldsJson())));
      }
      List<Map<String, Object>> imageMetadata = new ArrayList<>();
      Map<String, Object> values = new LinkedHashMap<>();
      for (NotebookField field : templates.get(version).fields()) {
        Object value = record.values().get(field.key());
        if (field.type().equals("IMAGE")) {
          var matching = images.metadataForKeys(record.id(), value instanceof List<?> keys
              ? keys : List.of());
          imageMetadata.addAll(matching);
          values.put(field.key(), matching);
        } else values.put(field.key(), value);
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", record.id()); row.put("occurredFrom", record.occurredFrom());
      row.put("occurredTo", record.occurredTo()); row.put("title", record.title());
      row.put("note", record.note()); row.put("templateVersion", version);
      row.put("values", values); row.put("createdByUserId", record.createdByUserId());
      row.put("updatedByUserId", record.updatedByUserId()); row.put("images", imageMetadata);
      rows.add(row);
    }
    access.requireExport(actor, eventId, from, to, timeZone);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("schemaVersion", "notebook-export/v1");
    payload.put("exportedAt", Instant.now().toString());
    payload.put("range", Map.of("from", from.toString(), "to", to.toString(), "timeZone", timeZone));
    payload.put("event", event);
    payload.put("templateVersions", List.copyOf(templates.values()));
    payload.put("records", rows);
    audit.record(actor, scope.ownerUserId(), eventId, null, mode.equals("COPY") ? "COPY" : "EXPORT");
    return payload;
  }

  private List<NotebookField> fields(String encoded) {
    try { return json.readValue(encoded, new TypeReference<List<NotebookField>>() {}); }
    catch (JsonProcessingException failure) { throw new IllegalStateException("Invalid export template", failure); }
  }

  private static BusinessException missing() {
    return new BusinessException(ErrorCode.NOT_FOUND, "Notebook export not found");
  }
}
