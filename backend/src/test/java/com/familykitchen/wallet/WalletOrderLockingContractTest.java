package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies every order wallet read-modify-write path takes deterministic database row locks. */
class WalletOrderLockingContractTest {

  @Test
  void bulkWalletLockQueryUsesDeterministicOrderBeforeForUpdate() throws Exception {
    String mapper = read("src/main/resources/mapper/wallet/WalletPersistenceMapper.xml");
    assertThat(mapper).contains("<select id=\"selectWalletsByMemberIdsForUpdate\"");
    assertThat(mapper).containsPattern(
        "(?s)selectWalletsByMemberIdsForUpdate.*where user_id in.*order by user_id.*for update");
  }

  @Test
  void orderMutationServicesUseOnlyLockedWalletSelectorsAndAreTransactional() throws Exception {
    for (String relative : new String[] {
        "src/main/java/com/familykitchen/order/service/impl/FamilyOrderApplicationServiceImpl.java",
        "src/main/java/com/familykitchen/order/service/impl/MerchantOrderApplicationServiceImpl.java"
    }) {
      String source = read(relative);
      assertThat(source).contains("walletMapper.selectWalletsByMemberIdsForUpdate(memberIds)");
      assertThat(source).doesNotContain("walletMapper.selectWalletsByMemberIds(memberIds)");
      assertThat(source).contains("@Transactional");
    }
    String merchant = read(
        "src/main/java/com/familykitchen/order/service/impl/MerchantOrderApplicationServiceImpl.java");
    assertThat(merchant).contains("walletMapper.selectWalletByMemberIdForUpdate(memberId)");
    assertThat(merchant).doesNotContain("walletMapper.selectWalletByMemberId(memberId)");
  }

  private static String read(String relative) throws Exception {
    return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
  }
}
