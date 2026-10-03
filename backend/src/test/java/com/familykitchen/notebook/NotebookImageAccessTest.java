package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.familykitchen.notebook.mapper.NotebookImageMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookImageService;
import com.familykitchen.notebook.service.NotebookAuditService;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Private image requests cannot bypass record authorization. */
class NotebookImageAccessTest {
  @Test void unknownImageDoesNotReadPrivateStorage() {
    var service = new NotebookImageService(mock(NotebookImageMapper.class),
        mock(NotebookRecordMapper.class), mock(NotebookAccessPolicy.class),
        mock(NotebookAuditService.class), Path.of("data/notebook-private"), Path.of("data/uploads"));
    assertThatThrownBy(() -> service.read(2, 999)).isInstanceOf(RuntimeException.class);
  }

  @Test void existingImageStillRequiresCompleteRecordReadAuthorization() {
    var images = mock(NotebookImageMapper.class);
    var records = mock(NotebookRecordMapper.class);
    var access = mock(NotebookAccessPolicy.class);
    var audit = mock(NotebookAuditService.class);
    when(images.find(5)).thenReturn(new NotebookImageMapper.Image(5, 9, 1,
        "00000000-0000-0000-0000-000000000001.png", "photo.png", "image/png", 12));
    Instant instant = Instant.parse("2026-01-03T00:00:00Z");
    when(records.findAny(9)).thenReturn(new NotebookRecordMapper.Row(9, 7, 1, 1, 1,
        instant, instant, "A", null, 1, "{}", 0));
    when(access.requireRecordRead(2, 7, instant, instant)).thenThrow(
        new com.familykitchen.common.error.BusinessException(
            com.familykitchen.common.error.ErrorCode.NOT_FOUND, "denied"));
    var service = new NotebookImageService(images, records, access, audit,
        Path.of("data/notebook-private"), Path.of("data/uploads"));
    assertThatThrownBy(() -> service.read(2, 5)).isInstanceOf(RuntimeException.class);
    verifyNoInteractions(audit);
  }
}
