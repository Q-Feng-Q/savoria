package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.notification.mapper.NotificationPersistenceMapper;
import com.familykitchen.order.mapper.OrderPersistenceMapper;
import com.familykitchen.order.model.dto.OrderStatusRequest;
import com.familykitchen.order.model.entity.OrderRecordEntity;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.service.OrderStateMachine;
import com.familykitchen.order.service.impl.MerchantOrderApplicationServiceImpl;
import com.familykitchen.wallet.service.FamilyWalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies all merchant order money transitions use one family wallet hold. */
class FamilyWalletOrderLifecycleTest {
  private final OrderPersistenceMapper orders=mock(OrderPersistenceMapper.class);
  private final FamilyWalletService wallet=mock(FamilyWalletService.class);
  private final NotificationPersistenceMapper notifications=mock(NotificationPersistenceMapper.class);
  private final CurrentUserContext merchant=new CurrentUserContext(
      9L,1L,null,null,null,Set.of("MERCHANT_ADMIN"),Set.of());
  private MerchantOrderApplicationServiceImpl service;

  @BeforeEach void setUp(){
    service=new MerchantOrderApplicationServiceImpl(
        new OrderStateMachine(),orders,wallet,notifications);
    when(orders.selectOrderItemsByOrderIds(List.of(77L))).thenReturn(List.of());
    when(orders.selectDeliverySnapshotsByOrderIds(List.of(77L))).thenReturn(List.of());
  }

  @Test void rejectReleasesTheFamilyHold(){
    order(OrderStatus.PENDING,new BigDecimal("114.00"));
    assertThat(service.reject(merchant,77L)).isEqualTo(OrderStatus.REJECTED);
    verify(wallet).release(2L,77L,9L,new BigDecimal("114.00"),"order:77:merchant-reject");
  }

  @Test void completingOrderCapturesTheFamilyHold(){
    order(OrderStatus.READY,new BigDecimal("114.00"));
    assertThat(service.advance(merchant,77L,new OrderStatusRequest(OrderStatus.DONE,null)))
        .isEqualTo(OrderStatus.DONE);
    verify(wallet).capture(2L,77L,9L,new BigDecimal("114.00"),"order:77:complete");
  }

  @Test void completingLegacyOrderDoesNotCaptureTheFamilyWallet(){
    order(OrderStatus.READY,new BigDecimal("114.00"),null);

    assertThat(service.advance(merchant,77L,new OrderStatusRequest(OrderStatus.DONE,null)))
        .isEqualTo(OrderStatus.DONE);

    verifyNoInteractions(wallet);
    verify(orders).updateOrder(argThat(row -> OrderStatus.DONE.name().equals(row.getStatus())));
    verify(notifications).insertNotification(
        "family",2L,"family","order","历史订单已完成","历史订单已完成 #77");
  }

  @Test void deliveryFeeIncreaseAppendsFreezeAndDecreaseReleases(){
    order(OrderStatus.PENDING,new BigDecimal("120.00"));
    service.adjustDeliveryFee(merchant,77L,new BigDecimal("8.00"),"fee-up-1");
    verify(wallet).appendFreeze(2L,77L,9L,new BigDecimal("2.00"),"order:77:fee:fee-up-1");

    order(OrderStatus.PENDING,new BigDecimal("120.00"));
    service.adjustDeliveryFee(merchant,77L,new BigDecimal("4.00"),"fee-down-1");
    verify(wallet).release(2L,77L,9L,new BigDecimal("2.00"),"order:77:fee:fee-down-1");
  }

  private void order(OrderStatus status,BigDecimal total){
    order(status,total,40L);
  }

  private void order(OrderStatus status,BigDecimal total,Long sourceCartId){
    OrderRecordEntity row=new OrderRecordEntity();row.setId(77L);row.setMerchantId(1L);
    row.setFamilyId(2L);row.setSubmitterMemberId(21L);row.setSourceCartId(sourceCartId);
    row.setExpectedMealTime(LocalDateTime.of(2026,8,28,12,30));
    row.setDeliveryMode("DELIVERY");row.setDeliveryFee(new BigDecimal("6.00"));
    row.setStatus(status.name());row.setTotalAmount(total);
    when(orders.selectOrderByMerchantIdForUpdate(1L,77L)).thenReturn(row);
  }
}
