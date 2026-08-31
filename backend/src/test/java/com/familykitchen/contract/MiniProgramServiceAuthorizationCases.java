package com.familykitchen.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.dto.CartMutationRequest;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.cart.service.impl.CartApplicationServiceImpl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.dto.UpdateFamilyInfoRequest;
import com.familykitchen.family.model.dto.UpdateMerchantFamilyProfileRequest;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.service.impl.FamilyApplicationServiceImpl;
import com.familykitchen.family.service.impl.MerchantFamilyApplicationServiceImpl;
import com.familykitchen.family.service.impl.MerchantFamilyMenuApplicationServiceImpl;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.order.mapper.OrderPersistenceMapper;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.order.service.OrderSubmissionService;
import com.familykitchen.order.service.impl.FamilyOrderApplicationServiceImpl;
import com.familykitchen.order.service.impl.MerchantOrderApplicationServiceImpl;
import com.familykitchen.system.service.SystemSettingService;
import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import com.familykitchen.wallet.model.dto.AdjustFamilyBalanceRequest;
import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.service.FamilyWalletService;
import com.familykitchen.wallet.service.PersonalWalletCutoverGuard;
import com.familykitchen.wallet.service.impl.FamilyWalletApplicationServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.provider.Arguments;

/** Real service scenarios for the mini-program resource authorization matrix. */
final class MiniProgramServiceAuthorizationCases {

  private static final long MERCHANT_ID = 11L;
  private static final long FAMILY_ID = 13L;
  private static final long FORGED_ID = 99L;
  private static final CurrentUserContext MEMBER = familyUser("member");
  private static final CurrentUserContext ADMIN = familyUser("owner");
  private static final CurrentUserContext MERCHANT = new CurrentUserContext(
      7L, MERCHANT_ID, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

  private MiniProgramServiceAuthorizationCases() { }

  static Stream<Arguments> cases() {
    return Stream.of(
        test("family-info-read", "MEMBER", "own-family", MiniProgramServiceAuthorizationCases::familyInfoRead),
        test("family-menu-read", "MEMBER", "own-family", MiniProgramServiceAuthorizationCases::familyMenuRead),
        test("family-cart-read", "MEMBER", "own-family", MiniProgramServiceAuthorizationCases::familyCartRead),
        test("family-wallet-read", "MEMBER", "own-family", MiniProgramServiceAuthorizationCases::familyWalletRead),
        test("family-info-update", "FAMILY_ADMIN", "own-family", MiniProgramServiceAuthorizationCases::familyInfoUpdate),
        test("cart-mutation", "MEMBER", "same-merchant-other-family", MiniProgramServiceAuthorizationCases::foreignCartMutation),
        test("dish-detail", "MEMBER", "other-merchant", MiniProgramServiceAuthorizationCases::foreignFamilyDish),
        test("family-order-read", "MEMBER", "same-merchant-other-family", MiniProgramServiceAuthorizationCases::foreignFamilyOrderRead),
        test("family-order-cancel", "FAMILY_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignFamilyOrderCancel),
        test("merchant-family-read", "MERCHANT_ADMIN", "same-merchant-other-family", MiniProgramServiceAuthorizationCases::merchantFamilyRead),
        test("merchant-family-read", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantFamilyRead),
        test("merchant-family-update", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantFamilyUpdate),
        test("merchant-menu-read", "MERCHANT_ADMIN", "same-merchant-other-family", MiniProgramServiceAuthorizationCases::merchantMenuRead),
        test("merchant-menu-read", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantMenuRead),
        test("merchant-menu-save", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantMenuSave),
        test("merchant-wallet-read", "MERCHANT_ADMIN", "same-merchant-other-family", MiniProgramServiceAuthorizationCases::merchantWalletRead),
        test("merchant-wallet-read", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantWalletRead),
        test("merchant-wallet-adjust", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantWalletAdjust),
        test("merchant-order-read", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantOrderRead),
        test("merchant-order-cancel", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantOrderCancel),
        test("merchant-dish-read", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantDishRead),
        test("merchant-dish-update", "MERCHANT_ADMIN", "other-merchant", MiniProgramServiceAuthorizationCases::foreignMerchantDishUpdate));
  }

  private static Arguments test(String resource, String role, String scope, Executable scenario) {
    return Arguments.of(resource, role, scope, scenario);
  }

  private static void familyInfoRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    FamilyRecord row = family(FAMILY_ID);
    when(families.selectFamily(null, FAMILY_ID)).thenReturn(row);
    var view = familyService(families, mock(DishMapper.class)).familyInfo(MEMBER);
    assertEquals("林家", view.familyName());
    verify(families).selectFamily(null, FAMILY_ID);
  }

  private static void familyMenuRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.selectFamilyMenuItems(MERCHANT_ID, FAMILY_ID)).thenReturn(List.of());
    assertThat(familyService(families, mock(DishMapper.class)).menuItems(MEMBER, null, null)).isEmpty();
    verify(families).selectFamilyMenuItems(MERCHANT_ID, FAMILY_ID);
  }

  private static void familyCartRead() {
    CartMapper carts = mock(CartMapper.class);
    CartEntity active = new CartEntity();
    active.setId(4L); active.setFamilyId(FAMILY_ID); active.setMerchantId(MERCHANT_ID);
    active.setVersion(0L); active.setStatus("active");
    when(carts.selectFamilyActiveCart(FAMILY_ID)).thenReturn(active);
    when(carts.selectCartItems(4L)).thenReturn(List.of());
    assertEquals(4L, cartService(carts).cart(MEMBER).cartId());
    verify(carts).selectFamilyActiveCart(FAMILY_ID);
  }

  private static void familyWalletRead() {
    FamilyWalletService wallet = mock(FamilyWalletService.class);
    when(wallet.get(FAMILY_ID)).thenReturn(wallet(FAMILY_ID));
    assertEquals(FAMILY_ID,
        walletService(wallet, mock(FamilyMapper.class)).summary(MEMBER).familyId());
    verify(wallet).get(FAMILY_ID);
  }

  private static void familyInfoUpdate() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.updateFamilyInfo(FAMILY_ID, "林家新桌", "周末聚餐")).thenReturn(1);
    familyService(families, mock(DishMapper.class)).updateFamilyInfo(
        ADMIN, new UpdateFamilyInfoRequest(" 林家新桌 ", " 周末聚餐 "));
    verify(families).updateFamilyInfo(FAMILY_ID, "林家新桌", "周末聚餐");
  }

  private static void foreignCartMutation() {
    CartMapper carts = mock(CartMapper.class);
    CartEntity active = new CartEntity();
    active.setId(4L); active.setFamilyId(FAMILY_ID); active.setMerchantId(MERCHANT_ID);
    active.setVersion(0L); active.setStatus("active");
    when(carts.selectAvailableDish(FAMILY_ID, 21L))
        .thenReturn(new CartDishSnapshot(21L, "番茄牛腩", BigDecimal.TEN));
    when(carts.selectFamilyActiveCart(FAMILY_ID)).thenReturn(active);
    when(carts.selectFamilyCart(FORGED_ID, FAMILY_ID)).thenReturn(null);
    BusinessException error = assertThrows(BusinessException.class, () ->
        cartService(carts).mutateItem(MEMBER,
            new CartMutationRequest(FORGED_ID, 0L, "forged-cart", 21L, 1, null)));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(carts, never()).bumpVersion(any(), any(), any(Long.class));
    verify(carts, never()).upsertSelection(any(), any(), any(Integer.class), any());
  }

  private static void foreignFamilyDish() {
    DishMapper dishes = mock(DishMapper.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> familyService(mock(FamilyMapper.class), dishes).dishDetail(MEMBER, FORGED_ID));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(dishes).selectDish(MERCHANT_ID, FORGED_ID);
    verify(dishes, never()).selectDishIngredients(FORGED_ID);
  }

  private static void foreignFamilyOrderRead() {
    OrderPersistenceMapper orders = mock(OrderPersistenceMapper.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> familyOrderService(orders, mock(FamilyWalletService.class)).detail(MEMBER, FORGED_ID));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(orders).selectOrderByFamilyId(FAMILY_ID, FORGED_ID);
  }

  private static void foreignFamilyOrderCancel() {
    OrderPersistenceMapper orders = mock(OrderPersistenceMapper.class);
    FamilyWalletService wallet = mock(FamilyWalletService.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> familyOrderService(orders, wallet).cancel(ADMIN, FORGED_ID, "取消"));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(orders).selectOrderByFamilyIdForUpdate(FAMILY_ID, FORGED_ID);
    verify(orders, never()).updateOrder(any());
    verify(wallet, never()).release(any(Long.class), any(Long.class), any(Long.class), any(), any());
  }

  private static void merchantFamilyRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.selectFamily(MERCHANT_ID, FAMILY_ID)).thenReturn(family(FAMILY_ID));
    when(families.selectAddresses(FAMILY_ID)).thenReturn(List.of());
    when(families.selectMembers(FAMILY_ID)).thenReturn(List.of());
    var result = new MerchantFamilyApplicationServiceImpl(families, new ObjectMapper())
        .detail(MERCHANT, FAMILY_ID);
    assertEquals(FAMILY_ID, result.familyId());
    verify(families).selectFamily(MERCHANT_ID, FAMILY_ID);
  }

  private static void foreignMerchantFamilyRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> new MerchantFamilyApplicationServiceImpl(families, new ObjectMapper())
            .detail(MERCHANT, FORGED_ID));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(families).selectFamily(MERCHANT_ID, FORGED_ID);
    verify(families, never()).selectAddresses(FORGED_ID);
  }

  private static void foreignMerchantFamilyUpdate() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.updateFamilyProfile(MERCHANT_ID, FORGED_ID, "伪造家庭", null, "[]"))
        .thenReturn(0);
    BusinessException error = assertThrows(BusinessException.class,
        () -> new MerchantFamilyApplicationServiceImpl(families, new ObjectMapper())
            .updateProfile(MERCHANT, FORGED_ID,
                new UpdateMerchantFamilyProfileRequest("伪造家庭", null, List.of())));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(families).updateFamilyProfile(MERCHANT_ID, FORGED_ID, "伪造家庭", null, "[]");
    verifyNoMoreInteractions(families);
  }

  private static void merchantMenuRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.selectFamilyMenuItems(MERCHANT_ID, FAMILY_ID)).thenReturn(List.of());
    assertThat(merchantMenuService(families).menu(MERCHANT, FAMILY_ID)).isEmpty();
    verify(families).selectFamilyMenuItems(MERCHANT_ID, FAMILY_ID);
  }

  private static void foreignMerchantMenuRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.selectFamilyMenuItems(MERCHANT_ID, FORGED_ID)).thenReturn(List.of());
    assertThat(merchantMenuService(families).menu(MERCHANT, FORGED_ID)).isEmpty();
    verify(families).selectFamilyMenuItems(MERCHANT_ID, FORGED_ID);
  }

  private static void foreignMerchantMenuSave() {
    FamilyMapper families = mock(FamilyMapper.class);
    when(families.countFamilyOwnership(MERCHANT_ID, FORGED_ID)).thenReturn(0);
    SaveFamilyMenuRequest request = new SaveFamilyMenuRequest(List.of(
        new SaveFamilyMenuRequest.MenuItem(21L, true, 1, BigDecimal.TEN)));
    BusinessException error = assertThrows(BusinessException.class,
        () -> merchantMenuService(families).saveMenu(MERCHANT, FORGED_ID, request));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(families, never()).deleteFamilyMenu(any());
    verify(families, never()).insertFamilyMenuItem(any(), any(), any(Boolean.class), any(Integer.class), any());
  }

  private static void merchantWalletRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    FamilyWalletService wallet = mock(FamilyWalletService.class);
    when(families.countFamilyOwnership(MERCHANT_ID, FAMILY_ID)).thenReturn(1);
    when(wallet.get(FAMILY_ID)).thenReturn(wallet(FAMILY_ID));
    assertEquals(FAMILY_ID, walletService(wallet, families).summaryForMerchant(MERCHANT, FAMILY_ID).familyId());
    verify(families).countFamilyOwnership(MERCHANT_ID, FAMILY_ID);
  }

  private static void foreignMerchantWalletRead() {
    FamilyMapper families = mock(FamilyMapper.class);
    FamilyWalletService wallet = mock(FamilyWalletService.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> walletService(wallet, families).summaryForMerchant(MERCHANT, FORGED_ID));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(families).countFamilyOwnership(MERCHANT_ID, FORGED_ID);
    verify(wallet, never()).get(any(Long.class));
  }

  private static void foreignMerchantWalletAdjust() {
    FamilyMapper families = mock(FamilyMapper.class);
    FamilyWalletService wallet = mock(FamilyWalletService.class);
    BusinessException error = assertThrows(BusinessException.class, () ->
        walletService(wallet, families).adjustForMerchant(MERCHANT, FORGED_ID,
            new AdjustFamilyBalanceRequest("forged-wallet", LedgerType.MANUAL_CREDIT,
                BigDecimal.ONE, "越权调账")));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(wallet, never()).manualCredit(any(Long.class), any(Long.class), any(), any(), any());
    verify(wallet, never()).manualDebit(any(Long.class), any(Long.class), any(), any(), any());
    verify(wallet, never()).get(any(Long.class));
  }

  private static void foreignMerchantOrderRead() {
    OrderPersistenceMapper orders = mock(OrderPersistenceMapper.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> merchantOrderService(orders, mock(FamilyWalletService.class)).detail(MERCHANT, FORGED_ID));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(orders).selectOrderByMerchantId(MERCHANT_ID, FORGED_ID);
  }

  private static void foreignMerchantOrderCancel() {
    OrderPersistenceMapper orders = mock(OrderPersistenceMapper.class);
    FamilyWalletService wallet = mock(FamilyWalletService.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> merchantOrderService(orders, wallet).cancel(MERCHANT, FORGED_ID, "取消"));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(orders).selectOrderByMerchantIdForUpdate(MERCHANT_ID, FORGED_ID);
    verify(orders, never()).updateOrder(any());
    verify(wallet, never()).release(any(Long.class), any(Long.class), any(Long.class), any(), any());
  }

  private static void foreignMerchantDishRead() {
    DishMapper dishes = mock(DishMapper.class);
    BusinessException error = assertThrows(BusinessException.class,
        () -> dishService(dishes).detail(MERCHANT, FORGED_ID));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(dishes).selectDish(MERCHANT_ID, FORGED_ID);
    verify(dishes, never()).selectDishIngredients(FORGED_ID);
  }

  private static void foreignMerchantDishUpdate() {
    DishMapper dishes = mock(DishMapper.class);
    BusinessException error = assertThrows(BusinessException.class, () ->
        dishService(dishes).updateDish(MERCHANT, FORGED_ID,
            new DishRequest("伪造菜", 3L, null, null, BigDecimal.TEN,
                List.of(), List.of(), "active")));
    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(dishes).lockMerchant(MERCHANT_ID);
    verify(dishes).selectDishForUpdate(MERCHANT_ID, FORGED_ID);
    verify(dishes, never()).updateDish(any());
    verify(dishes, never()).deleteDishIngredients(FORGED_ID);
    verify(dishes, never()).deleteCookingSteps(FORGED_ID);
  }

  private static CurrentUserContext familyUser(String role) {
    return new CurrentUserContext(7L, MERCHANT_ID, FAMILY_ID, 17L, role, Set.of(), Set.of());
  }

  private static FamilyApplicationServiceImpl familyService(
      FamilyMapper families, DishMapper dishes) {
    return new FamilyApplicationServiceImpl(families, dishes, new ObjectMapper());
  }

  private static CartApplicationServiceImpl cartService(CartMapper carts) {
    CommandIdempotencyService commands = mock(CommandIdempotencyService.class);
    when(commands.execute(any(), any())).thenAnswer(invocation -> {
      Supplier<CommandIdempotencyService.Result> action = invocation.getArgument(1);
      return action.get();
    });
    Clock clock = Clock.fixed(Instant.parse("2026-08-27T06:00:00Z"), ZoneId.of("Asia/Shanghai"));
    return new CartApplicationServiceImpl(carts, new ExpectedMealTimePolicy(clock), commands,
        new ObjectMapper().findAndRegisterModules());
  }

  private static FamilyOrderApplicationServiceImpl familyOrderService(
      OrderPersistenceMapper orders, FamilyWalletService wallet) {
    return new FamilyOrderApplicationServiceImpl(
        mock(OrderSubmissionService.class), orders, mock(CartMapper.class), mock(DishMapper.class),
        mock(FamilyMapper.class), mock(FamilyRelationMapper.class),
        mock(NotificationPersistenceMapper.class), new OrderStateMachine(),
        mock(ExpectedMealTimePolicy.class), wallet, mock(CommandIdempotencyService.class),
        mock(PersonalWalletCutoverGuard.class));
  }

  private static MerchantOrderApplicationServiceImpl merchantOrderService(
      OrderPersistenceMapper orders, FamilyWalletService wallet) {
    return new MerchantOrderApplicationServiceImpl(new OrderStateMachine(), orders, wallet,
        mock(NotificationPersistenceMapper.class));
  }

  private static MerchantFamilyMenuApplicationServiceImpl merchantMenuService(FamilyMapper families) {
    return new MerchantFamilyMenuApplicationServiceImpl(families, mock(DishApplicationService.class));
  }

  private static FamilyWalletApplicationServiceImpl walletService(
      FamilyWalletService wallet, FamilyMapper families) {
    return new FamilyWalletApplicationServiceImpl(wallet, mock(FamilyWalletMapper.class), families);
  }

  private static DishApplicationServiceImpl dishService(DishMapper dishes) {
    SystemSettingService settings = mock(SystemSettingService.class);
    when(settings.dishReviewEnabled()).thenReturn(false);
    return new DishApplicationServiceImpl(dishes, settings, mock(DishReviewService.class),
        new MerchantDishMutationLock(dishes), mock(FamilyMapper.class));
  }

  private static FamilyRecord family(long familyId) {
    FamilyRecord row = new FamilyRecord();
    row.setFamilyId(familyId); row.setFamilyName("林家"); row.setMerchantId(MERCHANT_ID);
    row.setMerchantName("老祁私厨"); row.setContactNamesJson("[]");
    return row;
  }

  private static FamilyWalletAccountDO wallet(long familyId) {
    FamilyWalletAccountDO row = new FamilyWalletAccountDO();
    row.familyId = familyId; row.availableAmount = BigDecimal.TEN;
    row.frozenAmount = BigDecimal.ZERO; row.version = 1L; row.updatedAt = LocalDateTime.now();
    return row;
  }
}
