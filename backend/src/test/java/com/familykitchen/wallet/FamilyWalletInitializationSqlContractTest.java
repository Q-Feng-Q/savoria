package com.familykitchen.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Guards the database-side safety properties of lazy wallet initialization. */
class FamilyWalletInitializationSqlContractTest {
  @Test
  void insertIsIdempotentZeroValuedAndLimitedToActiveFamilies() throws Exception {
    String xml = Files.readString(
        Path.of("src/main/resources/mapper/wallet/FamilyWalletMapper.xml"),
        StandardCharsets.UTF_8).replaceAll("\\s+", " ").toLowerCase();

    assertThat(xml).contains("<insert id=\"insertaccountifmissing\"");
    assertThat(xml).contains("insert ignore into family_wallets");
    assertThat(xml).contains("select id,0,0 from families");
    assertThat(xml).contains("where id=#{familyid} and status='active'");
  }
}
