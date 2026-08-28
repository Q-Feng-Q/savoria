package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.cart.service.ActiveCartMemberCleanupService;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Verifies that membership changes remove only invalid active-cart attribution. */
class FamilyMemberCartCleanupTest {

  @Test
  void removesOneMemberAndRecomputesTheSharedAggregateOnce() {
    CartMapper mapper = mock(CartMapper.class);
    FamilyRelationMapper relations = mock(FamilyRelationMapper.class);
    CartEntity cart = cart(4L, 7L);
    CartItemEntity kept = item(8L, 3);
    CartItemEntity removed = item(9L, 1);
    when(mapper.selectFamilyActiveCartForUpdate(13L)).thenReturn(cart);
    when(mapper.selectCartItemsForUpdate(4L)).thenReturn(List.of(kept, removed));
    when(mapper.selectSelectionsForUpdate(8L))
        .thenReturn(List.of(selection(21L, 1), selection(22L, 2)));
    when(mapper.selectSelectionsForUpdate(9L))
        .thenReturn(List.of(selection(21L, 1)));
    when(mapper.bumpVersion(4L, 13L, 7L)).thenReturn(1);

    boolean changed = new ActiveCartMemberCleanupService(mapper, relations).removeMember(13L, 21L);

    assertThat(changed).isTrue();
    verify(mapper).deleteSelection(8L, 21L);
    verify(mapper).deleteSelection(9L, 21L);
    verify(mapper).updateCartItem(kept);
    assertThat(kept.getQuantity()).isEqualTo(2);
    verify(mapper).deleteAggregateItem(9L);
    verify(mapper).bumpVersion(4L, 13L, 7L);
    verify(relations).lockActiveParticipants(13L, List.of(21L, 22L));
  }

  @Test
  void noSelectionLeavesVersionAndSubmittedSnapshotsUntouched() {
    CartMapper mapper = mock(CartMapper.class);
    when(mapper.selectFamilyActiveCartForUpdate(13L)).thenReturn(cart(4L, 7L));
    when(mapper.selectCartItemsForUpdate(4L)).thenReturn(List.of(item(8L, 3)));
    when(mapper.selectSelectionsForUpdate(8L)).thenReturn(List.of());

    boolean changed = new ActiveCartMemberCleanupService(
        mapper, mock(FamilyRelationMapper.class)).removeMember(13L, 21L);

    assertThat(changed).isFalse();
    verify(mapper, never()).bumpVersion(4L, 13L, 7L);
    verify(mapper, never()).deleteAggregateItem(8L);
  }

  @Test
  void familyCleanupLocksCartRowsBeforeDeletingAndVersioning() {
    CartMapper mapper = mock(CartMapper.class);
    FamilyRelationMapper relations = mock(FamilyRelationMapper.class);
    when(mapper.selectFamilyActiveCartForUpdate(13L)).thenReturn(cart(4L, 7L));
    when(mapper.selectCartItemsForUpdate(4L)).thenReturn(List.of(item(8L, 3)));
    when(mapper.selectSelectionsForUpdate(8L))
        .thenReturn(List.of(selection(21L, 1), selection(22L, 2)));
    when(mapper.bumpVersion(4L, 13L, 7L)).thenReturn(1);

    new ActiveCartMemberCleanupService(mapper, relations).removeFamily(13L);

    InOrder order = inOrder(mapper);
    order.verify(mapper).selectFamilyActiveCartForUpdate(13L);
    order.verify(mapper).selectCartItemsForUpdate(4L);
    order.verify(mapper).deleteAllSelections(4L);
    order.verify(mapper).deleteAllAggregateItems(4L);
    order.verify(mapper).bumpVersion(4L, 13L, 7L);
    verify(relations).lockActiveParticipants(13L, List.of(21L, 22L));
  }

  private static CartEntity cart(Long id, Long version) {
    CartEntity cart = new CartEntity();
    cart.setId(id);
    cart.setFamilyId(13L);
    cart.setStatus("active");
    cart.setVersion(version);
    return cart;
  }

  private static CartItemEntity item(Long id, int quantity) {
    CartItemEntity item = new CartItemEntity();
    item.setId(id);
    item.setCartId(4L);
    item.setDishId(31L);
    item.setDishNameSnapshot("番茄牛腩");
    item.setPrice(BigDecimal.TEN);
    item.setQuantity(quantity);
    return item;
  }

  private static CartItemSelectionEntity selection(Long userId, int quantity) {
    CartItemSelectionEntity selection = new CartItemSelectionEntity();
    selection.userId = userId;
    selection.quantity = quantity;
    return selection;
  }
}
