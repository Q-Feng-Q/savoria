package com.familykitchen.notebook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.controller.NotebookConfigController;
import com.familykitchen.system.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Notebook config is account-authenticated and exposes only the live month limit. */
class NotebookConfigControllerTest {
  @Test void configReadsCurrentGlobalLimitForAnAuthenticatedAccount() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var settings = mock(SystemSettingService.class);
    when(users.require(any(HttpServletRequest.class))).thenReturn(new CurrentUserContext(
        42L, null, null, 42L, "member", Set.of(), Set.of()));
    when(settings.notebookMaxQueryMonths()).thenReturn(24);
    var mvc = MockMvcBuilders.standaloneSetup(new NotebookConfigController(users, settings)).build();
    mvc.perform(get("/notebook/config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data.maxQueryMonths").value(24));
    verify(settings).notebookMaxQueryMonths();
  }
}
