package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.order.model.bo.CheckoutItem;
import com.familykitchen.order.model.bo.DeliverySnapshot;
import com.familykitchen.order.model.bo.FamilyDeliveryPolicy;
import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.service.OrderSubmissionService;
import com.familykitchen.order.service.impl.OrderSubmissionServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies aggregate order calculation without any personal-wallet mutation. */
class OrderSubmissionServiceTest {
  private final OrderSubmissionService service = new OrderSubmissionServiceImpl();

  @Test
  void createsOneAggregateItemAndPreservesMemberAttribution() {
    OrderSubmissionResult result = service.submit(command(DeliveryMode.DELIVERY));

    assertThat(result.order().status()).isEqualTo(OrderStatus.PENDING);
    assertThat(result.order().sourceCartId()).isEqualTo(40L);
    assertThat(result.order().expectedMealTime())
        .isEqualTo(LocalDateTime.of(2026, 8, 28, 12, 30));
    assertThat(result.order().totalAmount()).isEqualByComparingTo("120.00");
    assertThat(result.order().items()).singleElement().satisfies(item -> {
      assertThat(item.quantity()).isEqualTo(3);
      assertThat(item.amount()).isEqualByComparingTo("114.00");
      assertThat(item.ownerMemberId()).isNull();
      assertThat(item.selections()).containsExactly(
          new OrderSubmissionResult.MemberSelection(21L, "小林", 1, null),
          new OrderSubmissionResult.MemberSelection(22L, "阿禾", 2, "少盐"));
    });
    assertThat(result.memberCharges()).isEmpty();
    assertThat(result.walletChanges()).isEmpty();
  }

  @Test
  void pickupDoesNotChargeDeliveryFee() {
    OrderSubmissionResult result = service.submit(command(DeliveryMode.PICKUP));
    assertThat(result.order().deliveryFee()).isEqualByComparingTo("0.00");
    assertThat(result.order().totalAmount()).isEqualByComparingTo("114.00");
    assertThat(result.order().deliverySnapshot()).isNull();
  }

  @Test
  void rejectsAggregateQuantityThatDoesNotEqualSelections() {
    CheckoutItem invalid = new CheckoutItem(31L, "番茄牛腩", new BigDecimal("38.00"), 4,
        null, List.of(), List.of(
            new CheckoutItem.MemberSelection(21L, "小林", 1, null),
            new CheckoutItem.MemberSelection(22L, "阿禾", 2, null)));
    OrderCheckoutCommand command = new OrderCheckoutCommand(40L, 1L, 2L, 21L,
        LocalDateTime.of(2026, 8, 28, 12, 30), DeliveryMode.PICKUP, null, null,
        new FamilyDeliveryPolicy(true, new BigDecimal("6.00"), false), List.of(invalid));

    assertThatThrownBy(() -> service.submit(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("成员选择数量");
  }

  private static OrderCheckoutCommand command(DeliveryMode mode) {
    CheckoutItem item = new CheckoutItem(31L, "番茄牛腩", new BigDecimal("38.00"), 3,
        null, List.of(), List.of(
            new CheckoutItem.MemberSelection(21L, "小林", 1, null),
            new CheckoutItem.MemberSelection(22L, "阿禾", 2, "少盐")));
    return new OrderCheckoutCommand(40L, 1L, 2L, 21L,
        LocalDateTime.of(2026, 8, 28, 12, 30), mode, null,
        mode == DeliveryMode.DELIVERY
            ? new DeliverySnapshot("小林", "13800000000", "上海市徐汇区") : null,
        new FamilyDeliveryPolicy(true, new BigDecimal("6.00"), false), List.of(item));
  }
}
