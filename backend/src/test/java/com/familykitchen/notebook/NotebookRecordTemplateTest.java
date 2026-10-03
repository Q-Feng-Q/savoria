package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookImageMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import com.familykitchen.notebook.model.NotebookEventView;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookRecordService;
import com.familykitchen.notebook.service.NotebookRecordValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Collaborators may load only the current template needed to create a record. */
class NotebookRecordTemplateTest {
  @Test void currentTemplateRequiresLiveCreateGrantBeforeAndAfterReading() {
    var events = mock(NotebookEventMapper.class);
    var access = mock(NotebookAccessPolicy.class);
    when(access.requireCreateCapability(2, 7)).thenReturn(
        new NotebookAccessPolicy.Scope(1, null, null, false));
    when(events.findEvent(7)).thenReturn(new NotebookEventView(7, 1, "Private", null, null,
        0, false, false, 3));
    when(events.template(7, 3)).thenReturn(new NotebookEventMapper.TemplateRow(7, 3,
        "[{\"key\":\"field_a\",\"type\":\"TEXT\",\"label\":\"Notes\","
        + "\"required\":false,\"options\":[],\"unit\":null}]"));
    var service = new NotebookRecordService(mock(NotebookRecordMapper.class), events,
        new NotebookRecordValidator(), new NotebookRangePolicy(() -> 36),
        new ObjectMapper(), access, mock(NotebookImageMapper.class));
    var result = service.currentTemplate(2, 7);
    assertThat(result.eventId()).isEqualTo(7);
    assertThat(result.name()).isEqualTo("Private");
    assertThat(result.templateVersion()).isEqualTo(3);
    assertThat(result.fields()).hasSize(1);
    verify(access, times(2)).requireCreateCapability(2, 7);
    reset(access);
    when(access.requireCreateCapability(2, 7)).thenThrow(
        new BusinessException(ErrorCode.NOT_FOUND, "denied"));
    assertThatThrownBy(() -> service.currentTemplate(2, 7)).isInstanceOf(BusinessException.class);
  }
}
