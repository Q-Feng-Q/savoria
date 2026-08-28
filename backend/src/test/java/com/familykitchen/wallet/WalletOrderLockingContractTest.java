package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies every order wallet read-modify-write path takes deterministic database row locks. */
class WalletOrderLockingContractTest {

  @Test
  void retiredPersonalWalletMapperExposesNoMoneyMutationStatements() throws Exception {
    String mapper = read("src/main/resources/mapper/wallet/WalletPersistenceMapper.xml");
    assertThat(mapper).doesNotContain("selectWalletsByMemberIdsForUpdate",
        "selectWalletByMemberIdForUpdate", "updateWalletAmounts", "insertWalletLedger");
  }

  @Test
  void orderMutationServicesUseOnlyLockedWalletSelectorsAndAreTransactional() throws Exception {
    String family = read(
        "src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java");
    assertThat(family).contains("FamilyWalletService");
    assertThat(family).contains("wallet.freezeNewOrder(");
    assertThat(family).doesNotContain("WalletPersistenceMapper");
    assertThat(family).doesNotContain("selectWalletsByMemberIds");
    assertThat(family).contains("@Transactional");

    String merchant = read(
        "src/main/java/com/familykitchen/order/service/impl/MerchantOrderApplicationServiceImpl.java");
    assertThat(merchant).contains("FamilyWalletService");
    assertThat(merchant).contains("wallet.appendFreeze(", "wallet.release(", "wallet.capture(");
    assertThat(merchant).doesNotContain("WalletPersistenceMapper", "selectWalletByMemberId");
    assertThat(merchant).contains("@Transactional");

    String orderMapper = read("src/main/resources/mapper/order/OrderPersistenceMapper.xml");
    assertThat(orderMapper).containsPattern(
        "(?s)selectOrderByFamilyIdForUpdate.*family_id.*id.*for update");
    assertThat(orderMapper).containsPattern(
        "(?s)selectOrderByMerchantIdForUpdate.*merchant_id.*id.*for update");
  }

  private static String read(String relative) throws Exception {
    return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
  }
}
