package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.service.OrderStateMachine;
import org.junit.jupiter.api.Test;

/**
 * 验证订单StateMachine相关业务契约与回归场景。
 */
class OrderStateMachineTest {

  private final OrderStateMachine stateMachine = new OrderStateMachine();

  @Test
  void merchantCanMoveOrderThroughPreparationToDone() {
    assertThat(stateMachine.confirm(OrderStatus.PENDING)).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(stateMachine.advance(OrderStatus.CONFIRMED, OrderStatus.PREPARING, null))
        .isEqualTo(OrderStatus.PREPARING);
    assertThat(stateMachine.advance(OrderStatus.PREPARING, OrderStatus.READY, null))
        .isEqualTo(OrderStatus.READY);
    assertThat(stateMachine.advance(OrderStatus.READY, OrderStatus.DONE, null))
        .isEqualTo(OrderStatus.DONE);
  }

  @Test
  void familyCanCancelBeforePreparingOnly() {
    assertThat(stateMachine.familyCancel(OrderStatus.PENDING)).isEqualTo(OrderStatus.CANCELLED);
    assertThat(stateMachine.familyCancel(OrderStatus.CONFIRMED)).isEqualTo(OrderStatus.CANCELLED);

    assertThatThrownBy(() -> stateMachine.familyCancel(OrderStatus.PREPARING))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("家庭只能在备餐前取消订单");
  }

  @Test
  void merchantCancelPreparingRequiresReason() {
    assertThat(stateMachine.merchantCancel(OrderStatus.CONFIRMED, null))
        .isEqualTo(OrderStatus.CANCELLED);

    assertThatThrownBy(() -> stateMachine.merchantCancel(OrderStatus.PREPARING, " "))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("备餐中取消订单必须填写原因");

    assertThat(stateMachine.merchantCancel(OrderStatus.PREPARING, "ingredient unavailable"))
        .isEqualTo(OrderStatus.CANCELLED);
  }

  @Test
  void deliveryFeeCanOnlyBeAdjustedBeforeMerchantConfirmation() {
    stateMachine.validateDeliveryFeeAdjustment(OrderStatus.PENDING);

    assertThatThrownBy(() -> stateMachine.validateDeliveryFeeAdjustment(OrderStatus.CONFIRMED))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("配送费只能在确认前调整");
  }
}
