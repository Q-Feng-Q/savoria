package com.familykitchen.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Guards real-time identity lookup against inactive parent records. */
class IdentityContextMapperContractTest {
  @Test
  void identityLookupRequiresActiveFamilyAndMerchantParents() throws Exception {
    String sql = Files.readString(Path.of(
        "src/main/resources/mapper/common/IdentityContextMapper.xml"));

    assertThat(sql).contains("JOIN families f", "f.status='active'");
    assertThat(sql).contains("JOIN merchants m", "m.status='active'");
  }
}
