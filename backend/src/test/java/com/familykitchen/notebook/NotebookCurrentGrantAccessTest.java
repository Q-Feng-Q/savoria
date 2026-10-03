package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.model.NotebookEventView;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Mutating access checks must not trust an earlier repeatable-read snapshot. */
class NotebookCurrentGrantAccessTest {
  @Test void editLocksAccountsAndUsesCurrentGrantAfterAnOldSnapshot() {
    var events = mock(NotebookEventMapper.class);
    var contacts = mock(NotebookContactMapper.class);
    var grants = mock(NotebookGrantMapper.class);
    when(events.findEvent(7)).thenReturn(new NotebookEventView(7, 1, "Event", null, null,
        0, false, false, 1));
    when(events.lockEvent(7)).thenReturn(new NotebookEventView(7, 1, "Event", null, null,
        0, false, false, 1));
    var policy = new NotebookAccessPolicy(events, contacts, grants);
    var time = OffsetDateTime.parse("2026-01-03T10:00:00+08:00");
    assertThatThrownBy(() -> policy.requireEdit(2, 7, time, time))
        .isInstanceOf(BusinessException.class);
    var order = inOrder(contacts, events, grants);
    order.verify(contacts).lockPair(1, 2);
    order.verify(events).lockEvent(7);
    order.verify(grants).forGranteeCurrent(7, 2);
    verify(grants, never()).forGrantee(7, 2);
  }
}
