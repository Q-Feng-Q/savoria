package com.familykitchen.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.dto.CartMutationRequest;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.cart.service.impl.CartApplicationServiceImpl;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Verifies the shared cart's version, attribution, and absolute-quantity semantics. */
class CartApplicationServiceTest {
  private static final CurrentUserContext USER = new CurrentUserContext(
      7L, 11L, 13L, 7L, "member", Set.of(), Set.of());
  private static final CurrentUserContext FAMILY_ONLY_USER = new CurrentUserContext(
      7L, null, 13L, 7L, "member", Set.of(), Set.of());

  @Test
  void returnsAggregateDishWithCurrentMemberAndAllAttributions() {
    CartMapper mapper = mock(CartMapper.class);
    CartEntity cart = cart(4L, 3);
    CartItemEntity item = item(8L, 21L, 3);
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(cart);
    when(mapper.selectCartItems(4L)).thenReturn(List.of(item));
    when(mapper.selectSelections(8L)).thenReturn(List.of(
        selection(7L, "小林", 1), selection(9L, "阿禾", 2)));

    var view = service(mapper).cart(USER);

    assertThat(view.totalQuantity()).isEqualTo(3);
    assertThat(view.items().get(0).currentMemberQuantity()).isEqualTo(1);
    assertThat(view.items().get(0).selections())
        .extracting(selection -> selection.memberName() + " × " + selection.quantity())
        .containsExactly("小林 × 1", "阿禾 × 2");
  }

  @Test
  void keepsDeletedDishSnapshotVisibleAndMarksItUnavailable() {
    CartMapper mapper = mock(CartMapper.class);
    CartItemEntity item = item(8L, 21L, 3);
    item.setAvailable(false);
    item.setUnavailableReason("菜品已删除，不可提交");
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(cart(4L, 3));
    when(mapper.selectCartItems(4L)).thenReturn(List.of(item));
    when(mapper.selectSelections(8L)).thenReturn(List.of(selection(7L, "小林", 3)));

    var row = service(mapper).cart(USER).items().get(0);

    assertThat(row.available()).isFalse();
    assertThat(row.unavailableReason()).isEqualTo("菜品已删除，不可提交");
  }

  @Test
  void getCreatesOneEmptyCartUnderTheFamilyLock() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(null);
    when(mapper.lockFamilyForCart(13L)).thenReturn(11L);
    when(mapper.selectFamilyActiveCartForUpdate(13L)).thenReturn(null);
    when(mapper.insertCart(any())).thenAnswer(invocation -> {
      CartEntity created = invocation.getArgument(0);
      created.setId(4L);
      return 1;
    });
    when(mapper.selectCartItems(4L)).thenReturn(List.of());

    var view = service(mapper).cart(USER);

    assertThat(view.cartId()).isEqualTo(4L);
    assertThat(view.version()).isZero();
    ArgumentCaptor<CartEntity> inserted = ArgumentCaptor.forClass(CartEntity.class);
    verify(mapper).insertCart(inserted.capture());
    assertThat(inserted.getValue().getMerchantId()).isEqualTo(11L);
    assertThat(inserted.getValue().getMemberId()).isNull();
    assertThat(inserted.getValue().getMealSlotId()).isNull();
  }

  @Test
  void createsCartForOrdinaryFamilyMemberUsingFamiliesMerchant() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(null);
    when(mapper.lockFamilyForCart(13L)).thenReturn(11L);
    when(mapper.selectFamilyActiveCartForUpdate(13L)).thenReturn(null);
    when(mapper.insertCart(any())).thenAnswer(invocation -> {
      CartEntity created = invocation.getArgument(0);
      created.setId(4L);
      return 1;
    });
    when(mapper.selectCartItems(4L)).thenReturn(List.of());

    var view = service(mapper).cart(FAMILY_ONLY_USER);

    assertThat(view.cartId()).isEqualTo(4L);
    ArgumentCaptor<CartEntity> inserted = ArgumentCaptor.forClass(CartEntity.class);
    verify(mapper).insertCart(inserted.capture());
    assertThat(inserted.getValue().getMerchantId()).isEqualTo(11L);
  }

  @Test
  void setsAbsoluteMemberQuantityAndRecomputesAggregate() {
    CartMapper mapper = mock(CartMapper.class);
    CartEntity before = cart(4L, 3);
    CartEntity after = cart(4L, 4);
    CartItemEntity item = item(8L, 21L, 3);
    when(mapper.selectAvailableDish(13L, 21L))
        .thenReturn(new CartDishSnapshot(21L, "家庭价牛腩", new BigDecimal("12.00")));
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(before, after);
    when(mapper.selectFamilyCart(4L, 13L)).thenReturn(after);
    when(mapper.bumpVersion(4L, 13L, 3)).thenReturn(1);
    when(mapper.selectCartItem(4L, 21L)).thenReturn(item);
    when(mapper.sumSelections(8L)).thenReturn(5);
    when(mapper.selectCartItems(4L)).thenReturn(List.of());

    var view = service(mapper).mutateItem(
        USER, new CartMutationRequest(4L, 3L, "set-1", 21L, 2, "少盐"));

    verify(mapper).upsertSelection(8L, 7L, 2, "少盐");
    ArgumentCaptor<CartItemEntity> updated = ArgumentCaptor.forClass(CartItemEntity.class);
    verify(mapper).updateCartItem(updated.capture());
    assertThat(updated.getValue().getQuantity()).isEqualTo(5);
    assertThat(updated.getValue().getPrice()).isEqualByComparingTo("12.00");
    assertThat(updated.getValue().getDishNameSnapshot()).isEqualTo("家庭价牛腩");
    assertThat(view.version()).isEqualTo(4);
    InOrder locks = inOrder(mapper);
    locks.verify(mapper).lockMerchantForCart(11L);
    locks.verify(mapper).lockDishesForCart(11L, List.of(21L));
    locks.verify(mapper).lockFamilyMenuItemsForCart(13L, List.of(21L));
    locks.verify(mapper).bumpVersion(4L, 13L, 3L);
  }

  @Test
  void durableReplayReturnsTheOriginalResponseSnapshotWithoutReapplying() {
    CartMapper mapper = mock(CartMapper.class);
    CartEntity before = cart(4L, 0);
    CartEntity after = cart(4L, 1);
    when(mapper.selectAvailableDish(13L, 21L))
        .thenReturn(new CartDishSnapshot(21L, "番茄牛腩", BigDecimal.TEN));
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(before);
    when(mapper.selectFamilyCart(4L, 13L)).thenReturn(after);
    when(mapper.bumpVersion(4L, 13L, 0)).thenReturn(1);
    when(mapper.selectCartItem(4L, 21L)).thenReturn(null);
    when(mapper.sumSelections(any())).thenReturn(1);
    when(mapper.selectCartItems(4L)).thenReturn(List.of());
    CommandIdempotencyService commands = replayingCommands();
    CartApplicationServiceImpl service = service(mapper, commands);
    CartMutationRequest request = new CartMutationRequest(4L, 0L, "lost-response", 21L, 1, null);

    var original = service.mutateItem(USER, request);
    var replay = service.mutateItem(USER, request);

    assertThat(replay).isEqualTo(original);
    verify(mapper).bumpVersion(4L, 13L, 0);
    verify(mapper).insertCartItem(any());
  }

  @Test
  void zeroRemovesOnlyCurrentMemberAndPreservesOtherSelections() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(cart(4L, 3), cart(4L, 4));
    when(mapper.selectFamilyCart(4L, 13L)).thenReturn(cart(4L, 4));
    when(mapper.bumpVersion(4L, 13L, 3)).thenReturn(1);
    when(mapper.selectCartItem(4L, 21L)).thenReturn(item(8L, 21L, 3));
    when(mapper.sumSelections(8L)).thenReturn(2);
    when(mapper.selectCartItems(4L)).thenReturn(List.of());

    service(mapper).mutateItem(
        USER, new CartMutationRequest(4L, 3L, "remove-1", 21L, 0, null));

    verify(mapper).deleteSelection(8L, 7L);
    verify(mapper, never()).deleteAggregateItem(8L);
    verify(mapper).updateCartItem(any(CartItemEntity.class));
    verify(mapper, never()).selectAvailableDish(13L, 21L);
  }

  @Test
  void unavailableExistingDishCanBeReducedButNotIncreased() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(cart(4L, 3), cart(4L, 4));
    when(mapper.selectFamilyCart(4L, 13L)).thenReturn(cart(4L, 4));
    when(mapper.bumpVersion(4L, 13L, 3)).thenReturn(1);
    when(mapper.selectCartItem(4L, 21L)).thenReturn(item(8L, 21L, 3));
    when(mapper.selectSelectionForUpdate(8L, 7L)).thenReturn(selection(7L, "小林", 2));
    when(mapper.sumSelections(8L)).thenReturn(2);
    when(mapper.selectCartItems(4L)).thenReturn(List.of());

    service(mapper).mutateItem(
        USER, new CartMutationRequest(4L, 3L, "reduce-deleted", 21L, 1, null));

    verify(mapper).upsertSelection(8L, 7L, 1, null);
    verify(mapper, never()).selectAvailableDish(13L, 21L);
  }

  @Test
  void rejectsStaleVersionBeforeAnyCartWrite() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectAvailableDish(13L, 21L))
        .thenReturn(new CartDishSnapshot(21L, "番茄牛腩", BigDecimal.TEN));
    when(mapper.selectFamilyActiveCart(13L)).thenReturn(cart(4L, 4));

    assertThatThrownBy(() -> service(mapper).mutateItem(
        USER, new CartMutationRequest(4L, 3L, "stale-1", 21L, 1, null)))
        .isInstanceOf(BusinessException.class);

    verify(mapper, never()).bumpVersion(any(), any(), any(Long.class));
    verify(mapper, never()).upsertSelection(any(), any(), any(Integer.class), any());
  }

  private static CartApplicationServiceImpl service(CartMapper mapper) {
    CommandIdempotencyService commands = mock(CommandIdempotencyService.class);
    when(commands.execute(any(), any())).thenAnswer(invocation -> {
      Supplier<CommandIdempotencyService.Result> action = invocation.getArgument(1);
      return action.get();
    });
    return service(mapper, commands);
  }

  private static CartApplicationServiceImpl service(
      CartMapper mapper, CommandIdempotencyService commands) {
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-27T06:00:00Z"), ZoneId.of("Asia/Shanghai"));
    return new CartApplicationServiceImpl(
        mapper, new ExpectedMealTimePolicy(clock), commands,
        new ObjectMapper().findAndRegisterModules());
  }

  private static CommandIdempotencyService replayingCommands() {
    CommandIdempotencyService commands = mock(CommandIdempotencyService.class);
    AtomicReference<CommandIdempotencyService.Result> stored = new AtomicReference<>();
    when(commands.execute(any(), any())).thenAnswer(invocation -> {
      CommandIdempotencyService.Result prior = stored.get();
      if (prior != null) {
        return prior;
      }
      Supplier<CommandIdempotencyService.Result> action = invocation.getArgument(1);
      CommandIdempotencyService.Result result = action.get();
      stored.set(result);
      return result;
    });
    return commands;
  }

  private static CartEntity cart(long id, long version) {
    CartEntity cart = new CartEntity();
    cart.setId(id);
    cart.setMerchantId(11L);
    cart.setFamilyId(13L);
    cart.setStatus("active");
    cart.setVersion(version);
    return cart;
  }

  private static CartItemEntity item(long id, long dishId, int quantity) {
    CartItemEntity item = new CartItemEntity();
    item.setId(id);
    item.setCartId(4L);
    item.setDishId(dishId);
    item.setDishNameSnapshot("番茄牛腩");
    item.setPrice(BigDecimal.TEN);
    item.setQuantity(quantity);
    return item;
  }

  private static CartItemSelectionEntity selection(
      long memberId, String memberName, int quantity) {
    CartItemSelectionEntity selection = new CartItemSelectionEntity();
    selection.userId = memberId;
    selection.memberName = memberName;
    selection.quantity = quantity;
    return selection;
  }
}
