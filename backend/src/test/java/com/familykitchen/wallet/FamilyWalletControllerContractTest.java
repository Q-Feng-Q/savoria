package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies the public family-wallet HTTP surface no longer uses member identifiers. */
class FamilyWalletControllerContractTest {
  @Test void familyAndMerchantRoutesExposeSummaryLedgerAndAdjustment() throws Exception {
    String family = read("src/main/java/com/familykitchen/wallet/controller/FamilyWalletController.java");
    assertThat(family).contains("@RequestMapping(\"/family/wallet\")");
    assertThat(family).contains("@GetMapping", "@GetMapping(\"/ledgers\")");
    assertThat(family).doesNotContain("memberId");
    String merchant = read(
        "src/main/java/com/familykitchen/wallet/controller/MerchantFamilyWalletController.java");
    assertThat(merchant).contains("@RequestMapping(\"/merchant/families/{familyId}/wallet\")");
    assertThat(merchant).contains("@PostMapping(\"/adjust\")", "requestId()");
    assertThat(merchant).doesNotContain("memberId");
  }

  private static String read(String relative) throws Exception {
    return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
  }
}
