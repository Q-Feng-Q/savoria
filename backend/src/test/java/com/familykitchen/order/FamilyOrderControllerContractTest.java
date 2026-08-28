package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.order.controller.FamilyOrderController;
import com.familykitchen.order.model.dto.SubmitOrderRequest;
import com.familykitchen.order.service.LegacyOrderPayloadGuard;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PutMapping;

/** Verifies the immutable shared-cart order submission HTTP contract. */
class FamilyOrderControllerContractTest {

  @Test
  void submitRequestUsesVersionedSharedCartFieldsOnly() {
    Set<String> fields = Arrays.stream(SubmitOrderRequest.class.getRecordComponents())
        .map(RecordComponent::getName).collect(Collectors.toSet());

    assertThat(fields).contains("cartId", "cartVersion", "requestId", "deliveryMode",
        "addressId", "remark");
    assertThat(fields).doesNotContain("date", "mealSlotId", "serviceDate",
        "payerMemberId", "deliveryFeePayerUserId");
  }

  @Test
  void submittedOrdersCannotBeUpdated() {
    assertThat(Arrays.stream(FamilyOrderController.class.getDeclaredMethods())
        .filter(method -> method.isAnnotationPresent(PutMapping.class)))
        .isEmpty();
  }

  @Test
  void stableUpgradeErrorExistsForRetiredPayloads() throws Exception {
    Class<?> errorCode = Class.forName("com.familykitchen.common.error.ErrorCode");
    assertThat(Arrays.stream(errorCode.getEnumConstants()).map(String::valueOf))
        .contains("CLIENT_UPGRADE_REQUIRED");
    assertThat(Class.forName("com.familykitchen.order.service.LegacyOrderPayloadGuard"))
        .isNotNull();
  }

  @Test
  void everyRetiredWriteFieldIsRejectedBeforeBinding() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    for (String field : Set.of("mealSlotId", "serviceDate", "date",
        "payerMemberId", "deliveryFeePayerUserId")) {
      assertThatThrownBy(() -> LegacyOrderPayloadGuard.requireCompatible(
          mapper.readTree("{\"" + field + "\":1}")))
          .isInstanceOfSatisfying(BusinessException.class,
              failure -> assertThat(failure.errorCode())
                  .isEqualTo(ErrorCode.CLIENT_UPGRADE_REQUIRED));
    }
  }

  @Test
  void merchantStateChangesNeverRewriteSubmittedItemSnapshots() throws Exception {
    String source = Files.readString(Path.of(
        "src/main/java/com/familykitchen/order/service/impl/MerchantOrderApplicationServiceImpl.java"),
        StandardCharsets.UTF_8);
    assertThat(source).doesNotContain("orderMapper.deleteOrderItems(order.orderId())");
    assertThat(source).doesNotContain("orderMapper.deleteDeliverySnapshot(order.orderId())");
  }
}
