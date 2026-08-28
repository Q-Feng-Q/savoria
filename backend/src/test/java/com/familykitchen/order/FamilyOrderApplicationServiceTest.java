package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.entity.CartDishSnapshot;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.common.idempotency.CommandIdempotencyService;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.order.mapper.OrderPersistenceMapper;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.model.entity.OrderItemEntity;
import com.familykitchen.order.model.entity.OrderItemSelectionEntity;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.vo.OrderView;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.order.service.impl.FamilyOrderApplicationServiceImpl;
import com.familykitchen.order.service.impl.OrderSubmissionServiceImpl;
import com.familykitchen.wallet.service.FamilyWalletService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

/** Verifies the shared-cart submission transaction orchestration. */
class FamilyOrderApplicationServiceTest {
  @Test
  void snapshotsSelectionsFreezesFamilyWalletAndSubmitsExactCartVersion() {
    OrderPersistenceMapper orders=mock(OrderPersistenceMapper.class);
    CartMapper carts=mock(CartMapper.class);DishMapper dishes=mock(DishMapper.class);
    FamilyMapper families=mock(FamilyMapper.class);FamilyRelationMapper relations=mock(FamilyRelationMapper.class);
    NotificationPersistenceMapper notifications=mock(NotificationPersistenceMapper.class);
    FamilyWalletService wallet=mock(FamilyWalletService.class);
    CommandIdempotencyService commands=mock(CommandIdempotencyService.class);
    when(commands.execute(any(),any())).thenAnswer(this::execute);
    AtomicReference<OrderRecordEntity> orderRef=new AtomicReference<>();
    List<OrderItemEntity> savedItems=new ArrayList<>();
    List<OrderItemSelectionEntity> savedSelections=new ArrayList<>();
    when(orders.insertOrder(any())).thenAnswer(invocation->{
      OrderRecordEntity row=invocation.getArgument(0);row.setId(77L);orderRef.set(row);return 1;});
    when(orders.insertOrderItem(any())).thenAnswer(invocation->{
      OrderItemEntity row=invocation.getArgument(0);row.setId(78L);savedItems.add(row);return 1;});
    when(orders.insertOrderItemSelection(any())).thenAnswer(invocation->{
      savedSelections.add(invocation.getArgument(0));return 1;});
    when(orders.selectOrderByFamilyId(2L,77L)).thenAnswer(ignored->orderRef.get());
    when(orders.selectOrderItemsByOrderIds(List.of(77L))).thenAnswer(ignored->savedItems);
    when(orders.selectOrderItemSelections(List.of(78L))).thenAnswer(ignored->savedSelections);
    when(orders.selectDeliverySnapshotsByOrderIds(List.of(77L))).thenReturn(List.of());

    CartEntity cart=new CartEntity();cart.setId(40L);cart.setFamilyId(2L);cart.setMerchantId(1L);
    cart.setStatus("active");cart.setVersion(6L);
    cart.setExpectedMealTime(LocalDateTime.of(2026,8,28,12,30));
    CartItemEntity item=new CartItemEntity();item.setId(41L);item.setCartId(40L);item.setDishId(31L);
    item.setDishNameSnapshot("旧菜名");item.setPrice(BigDecimal.ONE);item.setQuantity(3);
    CartItemSelectionEntity first=selection(21L,1,null);
    CartItemSelectionEntity second=selection(22L,2,"少盐");
    when(carts.selectFamilyCartForUpdate(40L,2L)).thenReturn(cart);
    when(carts.selectCartItemsForUpdate(40L)).thenReturn(List.of(item));
    when(carts.selectSelectionsForUpdate(41L)).thenReturn(List.of(first,second));
    when(carts.selectAvailableDishForUpdate(2L,31L))
        .thenReturn(new CartDishSnapshot(31L,"番茄牛腩",new BigDecimal("38.00")));
    when(carts.submitFamilyCart(40L,2L,6L)).thenReturn(1);
    when(relations.lockActiveParticipants(2L,List.of(21L,22L))).thenReturn(List.of(21L,22L));
    when(families.selectMembers(2L)).thenReturn(List.of(member(21L,"小林"),member(22L,"阿禾")));
    FamilyRecord family=new FamilyRecord();family.setDeliveryEnabled(false);
    family.setDeliveryFeeDefault(BigDecimal.ZERO);family.setDeliveryFree(false);
    when(families.selectFamily(null,2L)).thenReturn(family);
    when(dishes.selectDishIngredients(31L)).thenReturn(List.of());

    ExpectedMealTimePolicy times=new ExpectedMealTimePolicy(Clock.fixed(
        Instant.parse("2026-08-28T02:00:00Z"),ZoneOffset.UTC));
    FamilyOrderApplicationServiceImpl service=new FamilyOrderApplicationServiceImpl(
        new OrderSubmissionServiceImpl(),orders,carts,dishes,families,relations,notifications,
        new OrderStateMachine(),times,wallet,commands);
    CurrentUserContext user=new CurrentUserContext(21L,1L,2L,21L,"member",Set.of(),Set.of());

    OrderView view=service.submit(user,new SubmitOrderRequest(40L,6L,"submit-1",
        DeliveryMode.PICKUP,null,null));

    assertThat(view.orderId()).isEqualTo(77L);
    assertThat(view.items()).singleElement().satisfies(row->{
      assertThat(row.dishName()).isEqualTo("番茄牛腩");
      assertThat(row.quantity()).isEqualTo(3);
      assertThat(row.selections()).extracting(OrderView.MemberSelectionView::memberName)
          .containsExactly("小林","阿禾");
    });
    verify(wallet).freezeNewOrder(2L,77L,21L,new BigDecimal("114.00"),"order:77:initial");
    verify(carts).submitFamilyCart(40L,2L,6L);
    verify(notifications).insertNotification(eq("merchant"),eq(1L),eq("merchant"),
        eq("order"),eq("收到新订单"),any());
  }

  private CommandIdempotencyService.Result execute(InvocationOnMock invocation){
    @SuppressWarnings("unchecked") Supplier<CommandIdempotencyService.Result> action=invocation.getArgument(1);
    return action.get();
  }
  private static CartItemSelectionEntity selection(Long user,int quantity,String remark){
    CartItemSelectionEntity row=new CartItemSelectionEntity();row.userId=user;row.quantity=quantity;
    row.itemRemark=remark;return row;
  }
  private static FamilyMemberRecord member(Long id,String name){
    FamilyMemberRecord row=new FamilyMemberRecord();row.setMemberId(id);row.setName(name);return row;
  }
}
