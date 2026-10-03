package com.familykitchen.notebook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.controller.NotebookRecordController;
import com.familykitchen.notebook.service.NotebookRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Record routes forward the authenticated account and explicit bounded dates. */
class NotebookRecordControllerTest {
  @Test void routesRecordsAndCalendarUsingAccountIdentity() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var service = mock(NotebookRecordService.class);
    when(users.require(any(HttpServletRequest.class))).thenReturn(new CurrentUserContext(
        42L, 9L, 8L, 42L, "owner", Set.of("PLATFORM_ADMIN"), Set.of("MERCHANT_ADMIN")));
    var mvc = MockMvcBuilders.standaloneSetup(new NotebookRecordController(users, service)).build();

    mvc.perform(get("/notebook/events/7/records?from=2026-01-01&to=2026-01-31&page=0&size=20"))
        .andExpect(status().isOk());
    mvc.perform(get("/notebook/events/7/calendar?from=2026-01-01&to=2026-01-31"))
        .andExpect(status().isOk());
    mvc.perform(get("/notebook/events/7/records")).andExpect(status().isBadRequest());
    mvc.perform(post("/notebook/events/7/records").contentType(MediaType.APPLICATION_JSON)
        .content("{\"occurredFrom\":\"2026-01-01T10:00:00+08:00\",\"occurredTo\":"
            + "\"2026-01-01T10:00:00+08:00\",\"title\":\"One\",\"values\":{}}"))
        .andExpect(status().isOk());
    mvc.perform(get("/notebook/records/5")).andExpect(status().isOk());
    mvc.perform(patch("/notebook/records/5").contentType(MediaType.APPLICATION_JSON)
        .content("{\"expectedVersion\":0,\"title\":\"Edited\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/notebook/records/5/upgrade-template").contentType(MediaType.APPLICATION_JSON)
        .content("{\"expectedVersion\":1,\"values\":{}}"))
        .andExpect(status().isOk());
    mvc.perform(delete("/notebook/records/5")).andExpect(status().isOk());

    verify(service).list(42L, 7L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 20);
    verify(service).calendar(42L, 7L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    verify(service).create(eq(42L), eq(7L), any());
    verify(service).get(42L, 5L);
    verify(service).update(eq(42L), eq(5L), any());
    verify(service).upgradeTemplate(eq(42L), eq(5L), eq(1), eq(java.util.Map.of()));
    verify(service).delete(42L, 5L);
  }
}
