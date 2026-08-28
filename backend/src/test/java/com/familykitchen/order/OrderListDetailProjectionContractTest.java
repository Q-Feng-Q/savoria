package com.familykitchen.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies order list endpoints omit member attribution while detail endpoints retain it. */
class OrderListDetailProjectionContractTest {
  /** Checks family and merchant services use explicit list/detail hydration modes.
   * @throws Exception when source cannot be read
   */
  @Test
  void listSkipsSelectionQueriesWhileDetailLoadsImmutableSnapshots() throws Exception {
    String family = Files.readString(Path.of("src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java"));
    String merchant = Files.readString(Path.of("src/main/java/com/familykitchen/order/service/impl/MerchantOrderApplicationServiceImpl.java"));
    assertThat(family).contains("selectOrdersByFamilyId(user.familyId()),false")
        .contains("hydrate(List.of(row),true)")
        .contains("!includeSelections||itemIds.isEmpty()");
    assertThat(merchant).contains("selectOrdersByMerchantId(merchantId), false")
        .contains("hydrate(List.of(order), true)")
        .contains("!includeSelections || itemIds.isEmpty()");
  }
}
