package com.familykitchen.system;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.system.controller.AdminSystemSettingController;
import com.familykitchen.system.model.dto.BrandSettingRequest;
import com.familykitchen.system.service.PlatformMailService;
import com.familykitchen.system.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BrandSettingControllerTest {
  @Test void permitsOnlyPlatformAdministrator() {
    var users = mock(CurrentUserProvider.class);
    var service = mock(SystemSettingService.class);
    var controller = new AdminSystemSettingController(users, service, mock(PlatformMailService.class));
    var request = mock(HttpServletRequest.class);
    var body = new BrandSettingRequest(null, null, null, null, null, null, null, null);
    for (var roles : java.util.List.of(Set.<String>of(), Set.of("MERCHANT_ADMIN"))) {
      when(users.require(request)).thenReturn(new CurrentUserContext(7L, null, null, null, null, roles, Set.of()));
      assertThatThrownBy(() -> controller.updateBranding(request, body)).isInstanceOf(BusinessException.class);
    }
    verifyNoInteractions(service);
    when(users.require(request)).thenReturn(new CurrentUserContext(7L, null, null, null, null, Set.of("PLATFORM_ADMIN"), Set.of()));
    controller.updateBranding(request, body);
    verify(service).updateBranding(7L, body);
  }

  @Test void patchRouteDeserializesNullableFieldsAndValidatesInput() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var service = mock(SystemSettingService.class);
    when(users.require(any())).thenReturn(new CurrentUserContext(7L, null, null, null, null, Set.of("PLATFORM_ADMIN"), Set.of()));
    var mvc = MockMvcBuilders.standaloneSetup(new AdminSystemSettingController(users, service, mock(PlatformMailService.class))).build();
    mvc.perform(patch("/admin/system-settings/branding").contentType("application/json")
        .content("{\"siteLogoUrl\":\"\",\"siteLogoSmallSize\":32}"))
        .andExpect(status().isOk());
    verify(service).updateBranding(7L, new BrandSettingRequest(null, "", null, null, null, 32, null, null));
    clearInvocations(service);
    mvc.perform(patch("/admin/system-settings/branding").contentType("application/json")
        .content("{\"siteLogoSmallUrl\":\"//evil.test/x\",\"siteLogoSize\":121}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test void rejectsFractionalSizesInsteadOfSilentlyTruncatingThem() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var service = mock(SystemSettingService.class);
    when(users.require(any())).thenReturn(new CurrentUserContext(7L, null, null, null, null, Set.of("PLATFORM_ADMIN"), Set.of()));
    var mvc = MockMvcBuilders.standaloneSetup(new AdminSystemSettingController(users, service, mock(PlatformMailService.class))).build();
    mvc.perform(patch("/admin/system-settings/branding").contentType("application/json")
        .content("{\"siteLogoSmallSize\":32.5}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
