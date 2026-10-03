package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.familykitchen.notebook.mapper.NotebookAuditMapper;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookAuditService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Audit reads are bounded and owner-only. */
class NotebookAuditTest {
  @Test void ownerAuditPageHasNoContentFields() {
    var mapper = mock(NotebookAuditMapper.class);
    var access = mock(NotebookAccessPolicy.class);
    when(mapper.page(eq(1L), eq(7L), any(), any(), eq(21), eq(0L))).thenReturn(List.of());
    var service = new NotebookAuditService(mapper, access, new NotebookRangePolicy(() -> 36));
    var result = service.list(1, 7, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
        "Asia/Shanghai", 0, 20);
    assertThat(result.items()).isEmpty();
    verify(access).requireOwner(1, 7);
    assertThatThrownBy(() -> service.list(1, 7, LocalDate.of(2023, 1, 1),
        LocalDate.of(2026, 1, 1), "Asia/Shanghai", 0, 20)).isInstanceOf(RuntimeException.class);
  }
}
