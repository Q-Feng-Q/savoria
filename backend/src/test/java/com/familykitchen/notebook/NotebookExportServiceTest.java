package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.model.NotebookEventView;
import com.familykitchen.notebook.model.NotebookRecordPage;
import com.familykitchen.notebook.model.NotebookRecordView;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookExportService;
import com.familykitchen.notebook.service.NotebookImageService;
import com.familykitchen.notebook.service.NotebookRecordService;
import com.familykitchen.notebook.service.NotebookAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Export contract and late authorization regression. */
class NotebookExportServiceTest {
  @Test void exportHasStableShapeAndRechecksPermissionBeforeReturning() {
    var access = mock(NotebookAccessPolicy.class);
    var events = mock(NotebookEventMapper.class);
    var records = mock(NotebookRecordService.class);
    var images = mock(NotebookImageService.class);
    var audit = mock(NotebookAuditService.class);
    var from = LocalDate.of(2026, 1, 1);
    var to = LocalDate.of(2026, 1, 31);
    when(access.requireExport(1, 7, from, to, "Asia/Shanghai"))
        .thenReturn(new NotebookAccessPolicy.Scope(1, null, null, true));
    when(events.findEvent(7)).thenReturn(new NotebookEventView(7, 1, "Journal", null, null, 0, false, false, 2));
    when(events.template(7, 1)).thenReturn(new NotebookEventMapper.TemplateRow(7, 1, "[]"));
    when(records.list(1, 7, from, to, "Asia/Shanghai", 0, 100)).thenReturn(new NotebookRecordPage(
        List.of(new NotebookRecordView(9, 7, 1, 1, 1, OffsetDateTime.parse("2026-01-01T10:00:00+08:00"),
            OffsetDateTime.parse("2026-01-01T10:00:00+08:00"), "First", null, 1,
            Map.of("field-a", "value"), List.of(), 0)), 0, 100, false));
    var service = new NotebookExportService(access, events, records, images, audit,
        new NotebookRangePolicy(() -> 36), new ObjectMapper());
    var payload = service.export(1, 7, from, to, "Asia/Shanghai", "FILE");
    assertThat(payload.get("schemaVersion")).isEqualTo("notebook-export/v1");
    assertThat((List<?>) payload.get("templateVersions")).hasSize(1);
    assertThat((List<?>) payload.get("records")).hasSize(1);
    verify(access, times(2)).requireExport(1, 7, from, to, "Asia/Shanghai");
    assertThatThrownBy(() -> service.export(1, 7, LocalDate.of(2023, 1, 1), to,
        "Asia/Shanghai", "COPY")).isInstanceOf(RuntimeException.class);
  }

  @Test void imageValueIsReplacedByMetadataAndLateRevokeReturnsNoPayload() {
    var access = mock(NotebookAccessPolicy.class);
    var events = mock(NotebookEventMapper.class);
    var records = mock(NotebookRecordService.class);
    var images = mock(NotebookImageService.class);
    var audit = mock(NotebookAuditService.class);
    var from = LocalDate.of(2026, 1, 1);
    var to = LocalDate.of(2026, 1, 31);
    var scope = new NotebookAccessPolicy.Scope(1,
        OffsetDateTime.parse("2026-01-01T00:00:00+08:00").toInstant(),
        OffsetDateTime.parse("2026-02-01T00:00:00+08:00").toInstant(), false);
    when(access.requireExport(2, 7, from, to, "Asia/Shanghai"))
        .thenReturn(scope, scope);
    when(events.findEvent(7)).thenReturn(new NotebookEventView(7, 1, "Private", null, null,
        0, false, false, 1));
    when(events.template(7, 1)).thenReturn(new NotebookEventMapper.TemplateRow(7, 1,
        "[{\"key\":\"photo\",\"type\":\"IMAGE\",\"label\":\"Photo\",\"required\":false,\"options\":[],\"unit\":null}]"));
    var row = new NotebookRecordView(9, 7, 1, 2, 2,
        OffsetDateTime.parse("2026-01-03T10:00:00+08:00"),
        OffsetDateTime.parse("2026-01-03T10:00:00+08:00"), "A", null, 1,
        Map.of("photo", List.of("private-key.png")), List.of(new NotebookField("photo", "IMAGE",
            "Photo", false, List.of(), null)), 0);
    when(records.list(2, 7, from, to, "Asia/Shanghai", 0, 100))
        .thenReturn(new NotebookRecordPage(List.of(row), 0, 100, false));
    when(images.metadataForKeys(9, List.of("private-key.png"))).thenReturn(List.of(
        Map.of("id", 4L, "originalName", "photo.png", "contentType", "image/png", "byteSize", 20L)));
    var service = new NotebookExportService(access, events, records, images, audit,
        new NotebookRangePolicy(() -> 36), new ObjectMapper());
    String exported = service.export(2, 7, from, to, "Asia/Shanghai", "COPY").toString();
    assertThat(exported).contains("photo.png").doesNotContain("private-key.png");
    verify(audit).record(2, 1, 7, null, "COPY");
    reset(access, audit);
    when(access.requireExport(2, 7, from, to, "Asia/Shanghai"))
        .thenReturn(scope).thenThrow(new com.familykitchen.common.error.BusinessException(
            com.familykitchen.common.error.ErrorCode.NOT_FOUND, "revoked"));
    assertThatThrownBy(() -> service.export(2, 7, from, to, "Asia/Shanghai", "COPY"))
        .isInstanceOf(com.familykitchen.common.error.BusinessException.class);
    verifyNoInteractions(audit);
  }
}
