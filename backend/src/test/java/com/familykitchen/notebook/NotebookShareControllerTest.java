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
import com.familykitchen.notebook.controller.NotebookContactController;
import com.familykitchen.notebook.controller.NotebookShareController;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookContactService;
import com.familykitchen.notebook.service.NotebookGrantService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Contact and grant routes use the authenticated account, never client ownership. */
class NotebookShareControllerTest {
  @Test void routesInvitationAndGrantActions() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var contacts = mock(NotebookContactService.class);
    var grants = mock(NotebookGrantService.class);
    var access = mock(NotebookAccessPolicy.class);
    when(users.require(any(HttpServletRequest.class))).thenReturn(new CurrentUserContext(
        42L, 9L, 8L, 42L, "owner", Set.of("PLATFORM_ADMIN"), Set.of("MERCHANT_ADMIN")));
    var mvc = MockMvcBuilders.standaloneSetup(new NotebookContactController(users, contacts),
        new NotebookShareController(users, grants, access)).build();

    mvc.perform(get("/notebook/contacts")).andExpect(status().isOk());
    mvc.perform(get("/notebook/contacts/invites")).andExpect(status().isOk());
    mvc.perform(post("/notebook/contacts/invites").contentType(MediaType.APPLICATION_JSON)
        .content("{\"identifier\":\"bob\"}")).andExpect(status().isOk());
    mvc.perform(post("/notebook/contacts/invites/3/accept").contentType(MediaType.APPLICATION_JSON)
        .content("{\"token\":\"secret\"}")).andExpect(status().isOk());
    mvc.perform(post("/notebook/contacts/invites/4/reject").contentType(MediaType.APPLICATION_JSON)
        .content("{\"token\":\"secret\"}")).andExpect(status().isOk());
    mvc.perform(delete("/notebook/contacts/2")).andExpect(status().isOk());
    mvc.perform(get("/notebook/events/7/grants")).andExpect(status().isOk());
    String grant = "{\"granteeUserId\":2,\"dataFrom\":\"2026-01-01\",\"dataTo\":"
        + "\"2026-01-31\",\"dataTimeZone\":\"Asia/Shanghai\",\"validFrom\":"
        + "\"2026-01-01T00:00:00Z\",\"validTo\":\"2027-01-01T00:00:00Z\"}";
    mvc.perform(post("/notebook/events/7/grants").contentType(MediaType.APPLICATION_JSON)
        .content(grant)).andExpect(status().isOk());
    mvc.perform(patch("/notebook/grants/8").contentType(MediaType.APPLICATION_JSON)
        .content(grant)).andExpect(status().isOk());
    mvc.perform(delete("/notebook/grants/8")).andExpect(status().isOk());
    mvc.perform(get("/notebook/shared")).andExpect(status().isOk());

    verify(contacts).list(42L);
    verify(contacts).received(42L);
    verify(contacts).invite(42L, "bob");
    verify(contacts).accept(42L, 3L, "secret");
    verify(contacts).reject(42L, 4L, "secret");
    verify(contacts).remove(42L, 2L);
    verify(grants).list(42L, 7L);
    verify(grants).create(eq(42L), eq(7L), any());
    verify(grants).update(eq(42L), eq(8L), any());
    verify(grants).revoke(42L, 8L);
    verify(access).shared(42L);
  }
}
