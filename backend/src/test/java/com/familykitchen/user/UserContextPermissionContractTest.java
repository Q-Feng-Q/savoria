package com.familykitchen.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.user.controller.UserController;
import com.familykitchen.user.model.vo.UserContextView;
import com.familykitchen.user.service.AccountLifecycleService;
import com.familykitchen.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies the explicit, ordered mini-program permission contract. */
@ExtendWith(MockitoExtension.class)
class UserContextPermissionContractTest {
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private UserProfileService userProfileService;
  @Mock private AccountLifecycleService accountLifecycleService;
  @Mock private HttpServletRequest request;

  @Test
  void unboundOrdinaryUserCanEnterFamilyOnboardingWithoutGrantedPermissions() {
    assertContext(context(2L, null, null, Set.of(), null, Set.of()),
        List.of("family"), List.of());
  }

  @Test
  void familyMemberGetsFamilyModeAndMembershipPermission() {
    assertContext(context(2L, 1L, "member", Set.of(), null, Set.of()),
        List.of("family"), List.of("FAMILY_MEMBER"));
  }

  @Test
  void familyOwnerGetsOrderedMemberAndAdminPermissions() {
    assertContext(context(2L, 1L, "OWNER", Set.of(), null, Set.of()),
        List.of("family"), List.of("FAMILY_MEMBER", "FAMILY_ADMIN"));
  }

  @Test
  void merchantOnlyUserGetsMerchantMode() {
    assertContext(context(2L, null, null, Set.of(), 9L, Set.of("MERCHANT_ADMIN")),
        List.of("merchant"), List.of("MERCHANT_ADMIN"));
  }

  @Test
  void combinedIdentityUsesStableModeAndPermissionOrdering() {
    Set<String> backendRoles = new HashSet<>(List.of("PLATFORM_ADMIN", "IGNORED"));
    Set<String> merchantScopes = new HashSet<>(List.of("merchant", "MERCHANT_ADMIN"));
    assertContext(context(2L, 1L, "admin", backendRoles, 9L, merchantScopes),
        List.of("family", "merchant"),
        List.of("FAMILY_MEMBER", "FAMILY_ADMIN", "MERCHANT_ADMIN", "PLATFORM_ADMIN"));
  }

  @Test
  void platformOnlyIdentityHasNoMiniProgramMode() {
    assertContext(context(2L, null, null, Set.of("platform_admin"), null, Set.of()),
        List.of(), List.of("PLATFORM_ADMIN"));
  }

  @Test
  void unknownRolesDoNotGrantAdminOrMerchantAccess() {
    assertContext(context(2L, 1L, "guest", Set.of("UNKNOWN"), 9L, Set.of("UNKNOWN")),
        List.of("family"), List.of("FAMILY_MEMBER"));
  }

  private void assertContext(CurrentUserContext current, List<String> modes,
      List<String> permissions) {
    when(currentUserProvider.require(request)).thenReturn(current);
    UserController controller =
        new UserController(currentUserProvider, userProfileService, accountLifecycleService);

    UserContextView result = controller.context(request).data();

    assertThat(result.availableModes()).containsExactlyElementsOf(modes);
    assertThat(result.permissionCodes()).containsExactlyElementsOf(permissions);
  }

  private CurrentUserContext context(Long userId, Long familyId, String familyRole,
      Set<String> backendRoles, Long merchantId, Set<String> merchantScopes) {
    return new CurrentUserContext(userId, merchantId, familyId,
        familyId == null ? null : userId, familyRole, backendRoles, merchantScopes);
  }
}
