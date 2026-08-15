package com.familykitchen.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.user.controller.UserController;
import com.familykitchen.user.model.vo.UserContextView;
import com.familykitchen.user.service.AccountLifecycleService;
import com.familykitchen.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 确保小程序账号上下文与请求鉴权使用同一份实时家庭身份。 */
@ExtendWith(MockitoExtension.class)
class UserContextRealtimeIdentityTest {
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private UserProfileService userProfileService;
  @Mock private AccountLifecycleService accountLifecycleService;
  @Mock private HttpServletRequest request;

  @Test
  void returnsRealtimeFamilyWhenLegacyProfileLookupIsStale() {
    when(currentUserProvider.require(request)).thenReturn(new CurrentUserContext(
        7L, null, 11L, 7L, "member", Set.of(), Set.of()));
    lenient().when(userProfileService.context(7L)).thenReturn(
        new UserContextView(7L, null, null, null, null, java.util.List.of()));
    UserController controller =
        new UserController(currentUserProvider, userProfileService, accountLifecycleService);

    UserContextView context = controller.context(request).data();

    assertThat(context.familyId()).isEqualTo(11L);
    assertThat(context.familyRole()).isEqualTo("member");
    verify(userProfileService, never()).context(7L);
  }
}
