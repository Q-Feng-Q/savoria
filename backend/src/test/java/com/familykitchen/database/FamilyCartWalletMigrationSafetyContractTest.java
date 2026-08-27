package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Static safety contracts that remain useful when Docker is unavailable. */
class FamilyCartWalletMigrationSafetyContractTest {
  private static final Path XML = Path.of(
      "src/main/resources/mapper/migration/FamilyCartWalletMigrationMapper.xml");
  private static final Path YAML = Path.of("src/main/resources/application.yml");

  @Test
  void compatibilityInstanceLeaseIsExplicitlyOptIn() throws Exception {
    String yaml = Files.readString(YAML);
    assertTrue(yaml.contains("FAMILY_KITCHEN_INSTANCE_LEASE_ENABLED:false"));
    assertFalse(yaml.contains("FAMILY_KITCHEN_INSTANCE_LEASE_ENABLED:true"));
  }

  @Test
  void mapperContainsAtomicRunnerLeaseAndProofGuards() throws Exception {
    String xml = Files.readString(XML).toLowerCase();
    assertTrue(xml.contains("id=\"acquirerunnerlease\""));
    assertTrue(xml.contains("id=\"lockandverifyrunnerlease\""));
    assertTrue(xml.contains("owner_token=#{ownertoken}"));
    assertTrue(xml.contains("lease_expires_at &gt; now()"));
    assertTrue(xml.contains("id=\"renewrunnerlease\""));
    assertTrue(xml.contains("id=\"releaserunnerlease\""));
    assertTrue(xml.contains("id=\"countpendingfamilies\""));
    assertTrue(xml.contains("id=\"countmissingexpectedholds\""));
  }

  @Test
  void preflightAndCartQueriesExposeEveryInvalidSourceInsteadOfInnerJoinOmission() throws Exception {
    String xml = Files.readString(XML).toLowerCase();
    for (String anomaly : new String[] {"wallet_amount_overflow", "wallet_precision_invalid",
        "wallet_inactive_family", "cart_user_missing", "cart_user_not_family_member", "cart_quantity_invalid",
        "cart_dish_unavailable", "order_frozen_unmapped"}) {
      assertTrue(xml.contains(anomaly), anomaly);
    }
    int cartItems = xml.indexOf("id=\"selectcartitems\"");
    int next = xml.indexOf("</select>", cartItems);
    String query = xml.substring(cartItems, next);
    assertTrue(query.contains("left join dishes"));
    assertTrue(query.contains("left join family_menu_items"));
  }

  @Test
  void finalizationUsesExactResumableFamilyKeyContract() throws Exception {
    String xml = Files.readString(XML).toLowerCase();
    assertTrue(xml.contains("active_family_id"));
    assertTrue(xml.contains("uk_carts_active_family(active_family_id)"));
    assertTrue(xml.contains("id=\"dropactivecartindex\""));
    assertTrue(xml.contains("id=\"dropactivecartcolumn\""));
    assertTrue(xml.contains("id=\"addactivefamilycolumn\""));
    assertTrue(xml.contains("id=\"addactivefamilyindex\""));
    assertTrue(xml.contains("id=\"generatedcolumnmetadata\""));
    assertTrue(xml.contains("id=\"exactuniqueindexexists\""));
    assertTrue(xml.contains("max(non_unique)=0"));
  }

  @Test
  void barrierPreflightAndPerFamilyConservationArePersistedExactly() throws Exception {
    String xml = Files.readString(XML).toLowerCase();
    assertTrue(xml.contains("else 'barrier_preflight' end"));
    assertTrue(xml.contains("id=\"removeineligiblefamilies\""));
    assertTrue(xml.contains("sum(s.source_available_amount)"));
    assertTrue(xml.contains("max(fw.available_amount)"));
    assertTrue(xml.contains("sum(s.source_frozen_amount)"));
    assertTrue(xml.contains("max(fw.frozen_amount)"));
    assertTrue(xml.contains("item_remark_conflict"));
  }
}
