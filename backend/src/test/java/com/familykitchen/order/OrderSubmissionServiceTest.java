package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.order.model.bo.CheckoutIngredient;
import com.familykitchen.order.model.bo.CheckoutItem;
import com.familykitchen.order.model.bo.DeliverySnapshot;
import com.familykitchen.order.model.bo.FamilyDeliveryPolicy;
import com.familykitchen.order.model.bo.OrderCheckoutCommand;
import com.familykitchen.order.model.bo.OrderSubmissionResult;
import com.familykitchen.order.model.enums.DeliveryMode;
import com.familykitchen.order.model.enums.OrderStatus;
import com.familykitchen.order.service.OrderSubmissionService;
import com.familykitchen.order.service.impl.OrderSubmissionServiceImpl;
import com.familykitchen.purchase.model.enums.IngredientCalcType;
import com.familykitchen.wallet.model.bo.WalletAccount;
import com.familykitchen.wallet.model.enums.LedgerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 验证订单SubmissionService相关业务契约与回归场景。
 */
class OrderSubmissionServiceTest {

  private final OrderSubmissionService service = new OrderSubmissionServiceImpl();

  @Test
  void submitOrderFreezesEachMemberDishAmountAndSubmitterDeliveryFee() {
    OrderCheckoutCommand command = new OrderCheckoutCommand(
        1L,
        2L,
        10L,
        20L,
        LocalDate.of(2026, 6, 30),
        DeliveryMode.DELIVERY,
        "Less oil please",
        new DeliverySnapshot("Chen Mei", "13800000000", "1 Tianyaoqiao Rd, Shanghai"),
        new FamilyDeliveryPolicy(true, new BigDecimal("6.00"), false),
        List.of(
            new CheckoutItem(
                100L,
                "Tomato Egg",
                10L,
                "Chen Mei",
                new BigDecimal("18.00"),
                2,
                null,
                List.of(new CheckoutIngredient("Tomato", new BigDecimal("2.00"), "pcs", IngredientCalcType.PER_PERSON))
            ),
            new CheckoutItem(
                101L,
                "Mapo Tofu",
                11L,
                "Chen Feng",
                new BigDecimal("19.00"),
                1,
                "No cilantro",
                List.of(new CheckoutIngredient("Tofu", new BigDecimal("1.00"), "block", IngredientCalcType.FIXED))
            )
        )
    );

    Map<Long, WalletAccount> wallets = wallets(
        wallet(10L, "100.00"),
        wallet(11L, "50.00")
    );

    OrderSubmissionResult result = service.submit(command, wallets);

    assertThat(result.order().status()).isEqualTo(OrderStatus.PENDING);
    assertThat(result.order().deliveryFee()).isEqualByComparingTo("6.00");
    assertThat(result.order().totalAmount()).isEqualByComparingTo("61.00");
    assertThat(result.memberCharges()).hasSize(2);
    assertThat(result.memberCharges())
        .filteredOn(charge -> charge.memberId().equals(10L))
        .singleElement()
        .satisfies(charge -> {
          assertThat(charge.dishAmount()).isEqualByComparingTo("36.00");
          assertThat(charge.deliveryFeeAmount()).isEqualByComparingTo("6.00");
          assertThat(charge.totalAmount()).isEqualByComparingTo("42.00");
        });
    assertThat(result.memberCharges())
        .filteredOn(charge -> charge.memberId().equals(11L))
        .singleElement()
        .satisfies(charge -> {
          assertThat(charge.dishAmount()).isEqualByComparingTo("19.00");
          assertThat(charge.deliveryFeeAmount()).isEqualByComparingTo("0.00");
          assertThat(charge.totalAmount()).isEqualByComparingTo("19.00");
        });
    assertThat(result.walletChanges()).hasSize(2);
    assertThat(result.walletChanges())
        .allSatisfy(change -> assertThat(change.type()).isEqualTo(LedgerType.FREEZE));
    assertThat(wallets.get(10L).balanceAmount()).isEqualByComparingTo("58.00");
    assertThat(wallets.get(10L).frozenAmount()).isEqualByComparingTo("42.00");
    assertThat(wallets.get(11L).balanceAmount()).isEqualByComparingTo("31.00");
    assertThat(wallets.get(11L).frozenAmount()).isEqualByComparingTo("19.00");
    assertThat(result.purchaseDemands()).singleElement().satisfies(demand -> {
      assertThat(demand.orderStatus()).isEqualTo(com.familykitchen.purchase.model.enums.OrderSourceStatus.PENDING);
      assertThat(demand.ingredients()).hasSize(2);
    });
    assertThat(result.notifications()).singleElement().satisfies(notification -> {
      assertThat(notification.scope()).isEqualTo("merchant");
      assertThat(notification.category()).isEqualTo("order");
    });
  }

  @Test
  void submitOrderUsesPickupWhenDeliveryDisabled() {
    OrderCheckoutCommand command = new OrderCheckoutCommand(
        1L,
        2L,
        10L,
        20L,
        LocalDate.of(2026, 6, 30),
        DeliveryMode.PICKUP,
        null,
        null,
        new FamilyDeliveryPolicy(false, new BigDecimal("6.00"), false),
        List.of(
            new CheckoutItem(
                100L,
                "Tomato Egg",
                10L,
                "Chen Mei",
                new BigDecimal("18.00"),
                1,
                null,
                List.of()
            )
        )
    );

    Map<Long, WalletAccount> wallets = wallets(wallet(10L, "20.00"));

    OrderSubmissionResult result = service.submit(command, wallets);

    assertThat(result.order().deliveryMode()).isEqualTo(DeliveryMode.PICKUP);
    assertThat(result.order().deliveryFee()).isEqualByComparingTo("0.00");
    assertThat(result.order().deliverySnapshot()).isNull();
  }

  @Test
  void submitOrderRejectsWhenAnyMemberBalanceIsInsufficient() {
    OrderCheckoutCommand command = new OrderCheckoutCommand(
        1L,
        2L,
        10L,
        20L,
        LocalDate.of(2026, 6, 30),
        DeliveryMode.DELIVERY,
        null,
        new DeliverySnapshot("Chen Mei", "13800000000", "1 Tianyaoqiao Rd, Shanghai"),
        new FamilyDeliveryPolicy(true, new BigDecimal("6.00"), false),
        List.of(
            new CheckoutItem(
                100L,
                "Tomato Egg",
                11L,
                "Chen Feng",
                new BigDecimal("18.00"),
                3,
                null,
                List.of()
            )
        )
    );

    Map<Long, WalletAccount> wallets = wallets(
        wallet(10L, "100.00"),
        wallet(11L, "20.00")
    );

    assertThatThrownBy(() -> service.submit(command, wallets))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("成员余额不足");
  }

  private static WalletAccount wallet(Long memberId, String balance) {
    return new WalletAccount(memberId, new BigDecimal(balance), BigDecimal.ZERO);
  }

  private static Map<Long, WalletAccount> wallets(WalletAccount... accounts) {
    Map<Long, WalletAccount> result = new LinkedHashMap<>();
    for (WalletAccount account : accounts) {
      result.put(account.memberId(), account);
    }
    return result;
  }
}
