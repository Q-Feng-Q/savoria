package com.familykitchen.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.auth.security.TokenService;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.common.security.IdentityContextMapper;
import com.familykitchen.common.security.IdentityContextRow;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Executable boundary matrix for every ID-bearing mini-program resource family. */
class MiniProgramResourceAuthorizationTest {

  @Test
  void anonymousRequestStopsBeforeTokenOrResourceLookup() {
    TokenService tokens = mock(TokenService.class);
    IdentityContextMapper identities = mock(IdentityContextMapper.class);
    SessionService sessions = mock(SessionService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    assertThatThrownBy(() -> new CurrentUserProvider(tokens, identities, sessions).require(request))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

    verify(tokens, never()).parse(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void expiredOrRemovedRealtimeIdentityCannotUseTokenSnapshotPermissions() {
    TokenService tokens = mock(TokenService.class);
    IdentityContextMapper identities = mock(IdentityContextMapper.class);
    SessionService sessions = mock(SessionService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(tokens.parse("token")).thenReturn(new CurrentUserContext(
        7L, 31L, 41L, 7L, "owner", Set.of("MERCHANT_ADMIN"), Set.of(), "s-1"));
    when(identities.findIdentity(7L)).thenReturn(null);

    assertThatThrownBy(() -> new CurrentUserProvider(tokens, identities, sessions).require(request))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

    verify(sessions).requireActive(7L, "s-1");
    verify(identities).findIdentity(7L);
    verify(identities, never()).findMerchantScopes(
        org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void switchedIdentityUsesRealtimeTenantAndNeverTheOldTokenSnapshot() {
    TokenService tokens = mock(TokenService.class);
    IdentityContextMapper identities = mock(IdentityContextMapper.class);
    SessionService sessions = mock(SessionService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(tokens.parse("token")).thenReturn(new CurrentUserContext(
        7L, 11L, 13L, 7L, "member", Set.of(), Set.of(), "s-1"));
    IdentityContextRow current = new IdentityContextRow();
    current.setUserId(7L);
    current.setMerchantId(22L);
    current.setFamilyId(24L);
    current.setFamilyRole("ADMIN");
    when(identities.findIdentity(7L)).thenReturn(current);
    when(identities.findPlatformRoles(7L)).thenReturn(List.of());
    when(identities.findMerchantScopes(7L, 22L)).thenReturn(List.of("MERCHANT_ADMIN"));

    CurrentUserContext result = new CurrentUserProvider(tokens, identities, sessions).require(request);

    assertThat(result.merchantId()).isEqualTo(22L);
    assertThat(result.familyId()).isEqualTo(24L);
    assertThat(result.roleTemplate()).isEqualTo("admin");
    assertThat(result.merchantAdminScopes()).containsExactly("MERCHANT_ADMIN");
    verify(identities, never()).findMerchantScopes(7L, 11L);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("resourceBoundaryContracts")
  void everyReadAndMutationResolvesIdsInsideTheAuthenticatedTenant(
      String matrixName, String relativePath, List<String> requiredSnippets,
      List<String> forbiddenSnippets) throws Exception {
    String source = normalizeWhitespace(Files.readString(mainSource(relativePath)));
    requiredSnippets.forEach(snippet -> assertThat(source).as(matrixName)
        .contains(normalizeWhitespace(snippet)));
    forbiddenSnippets.forEach(snippet -> assertThat(source).as(matrixName)
        .doesNotContain(normalizeWhitespace(snippet)));
  }

  @ParameterizedTest(name = "{0} [{1}/{2}]")
  @MethodSource("serviceBoundaryCases")
  void serviceBoundariesEnforceAuthenticatedTenantAndPreventRejectedWrites(
      String resource, String role, String scope, Executable scenario) throws Throwable {
    scenario.execute();
  }

  static Stream<Arguments> serviceBoundaryCases() {
    return MiniProgramServiceAuthorizationCases.cases();
  }

  static Stream<Arguments> resourceBoundaryContracts() {
    return Stream.of(
        Arguments.of("family-member/order/read+mutation/own-family",
            "order/service/impl/FamilyOrderApplicationServiceImpl.java",
            List.of("selectOrderByFamilyId(familyId,orderId)",
                "selectOrderByFamilyIdForUpdate(familyId,orderId)"),
            List.of("selectOrderById(orderId)")),
        Arguments.of("merchant-admin/order/read+mutation/own-merchant",
            "order/service/impl/MerchantOrderApplicationServiceImpl.java",
            List.of("selectOrderByMerchantId(merchantId, orderId)",
                "selectOrderByMerchantIdForUpdate(\n        user.merchantId(), orderId)"),
            List.of("selectOrderById(orderId)")),
        Arguments.of("family-member/cart/read+mutation/own-family",
            "cart/service/impl/CartApplicationServiceImpl.java",
            List.of("selectFamilyCartForUpdate(cart.getId(), cart.getFamilyId())",
                "selectAvailableDish(familyId, dishId)"),
            List.of("selectCartForUpdate(cart.getId())")),
        Arguments.of("merchant-admin/family/read+mutation/own-merchant",
            "family/service/impl/MerchantFamilyApplicationServiceImpl.java",
            List.of("selectFamily(merchantId, familyId)",
                "updateFamilyProfile(\n        user.merchantId(),\n        familyId"),
            List.of("selectFamily(null, familyId)")),
        Arguments.of("merchant-admin/menu/read+mutation/own-merchant",
            "family/service/impl/MerchantFamilyMenuApplicationServiceImpl.java",
            List.of("selectFamilyMenuItems(user.merchantId(), familyId)",
                "countFamilyOwnership(merchantId, familyId)",
                "lockMerchantForMenu(user.merchantId())",
                "selectOwnedDishesForUpdate(merchantId, sortedIds)"),
            List.of("selectFamilyMenuItems(null, familyId)")),
        Arguments.of("merchant-admin/wallet/read+mutation/own-merchant",
            "wallet/service/impl/FamilyWalletApplicationServiceImpl.java",
            List.of("countFamilyOwnership(user.merchantId(), familyId)"),
            List.of("wallet.get(familyId); return summary(familyId)")),
        Arguments.of("merchant-admin/dish/read+mutation/own-merchant",
            "dish/service/impl/DishApplicationServiceImpl.java",
            List.of("dishMapper.selectDish(merchantId, dishId)",
                "countCategoryOwnership(merchantId, categoryId)"),
            List.of("dishMapper.selectById(dishId)")));
  }

  private static Path mainSource(String relative) {
    Path cwd = Path.of(System.getProperty("user.dir"));
    Path backend = Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("backend");
    return backend.resolve("src/main/java/com/familykitchen").resolve(relative);
  }

  private static String normalizeWhitespace(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }
}
