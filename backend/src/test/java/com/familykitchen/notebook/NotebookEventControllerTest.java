package com.familykitchen.notebook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.controller.NotebookEventController;
import com.familykitchen.notebook.service.NotebookEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP routes always use the authenticated account, regardless of family or platform role. */
class NotebookEventControllerTest {
  @Test void routesEventAndTemplateRequestsUsingAccountIdentity() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var service = mock(NotebookEventService.class);
    when(users.require(any(HttpServletRequest.class))).thenReturn(new CurrentUserContext(
        42L, 9L, 8L, 42L, "owner", Set.of("PLATFORM_ADMIN"), Set.of("MERCHANT_ADMIN")));
    when(service.list(42L, false)).thenReturn(List.of());
    when(service.templates(42L, 7L)).thenReturn(List.of());
    var mvc = MockMvcBuilders.standaloneSetup(new NotebookEventController(users, service)).build();

    mvc.perform(get("/notebook/events")).andExpect(status().isOk());
    mvc.perform(post("/notebook/events").contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Journal\",\"fields\":[]}"))
        .andExpect(status().isOk());
    mvc.perform(get("/notebook/events/7")).andExpect(status().isOk());
    mvc.perform(patch("/notebook/events/7").contentType(MediaType.APPLICATION_JSON)
        .content("{\"archived\":true}"))
        .andExpect(status().isOk());
    mvc.perform(get("/notebook/events/7/delete-impact")).andExpect(status().isOk());
    mvc.perform(delete("/notebook/events/7?confirm=true")).andExpect(status().isOk());
    mvc.perform(put("/notebook/events/order").contentType(MediaType.APPLICATION_JSON)
        .content("{\"eventIds\":[7]}"))
        .andExpect(status().isOk());
    mvc.perform(get("/notebook/events/7/templates")).andExpect(status().isOk());
    mvc.perform(post("/notebook/events/7/templates").contentType(MediaType.APPLICATION_JSON)
        .content("{\"fields\":[]}"))
        .andExpect(status().isOk());

    verify(service).list(42L, false);
    verify(service).create(org.mockito.ArgumentMatchers.eq(42L), any());
    verify(service).get(42L, 7L);
    verify(service).update(org.mockito.ArgumentMatchers.eq(42L),
        org.mockito.ArgumentMatchers.eq(7L), any());
    verify(service).deleteImpact(42L, 7L);
    verify(service).delete(42L, 7L, true);
    verify(service).reorder(42L, List.of(7L));
    verify(service).templates(42L, 7L);
    verify(service).publishTemplate(org.mockito.ArgumentMatchers.eq(42L),
        org.mockito.ArgumentMatchers.eq(7L), any());
  }
}
