package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies that a merchant can configure every active dish for a family. */
class FamilyMenuMapperContractTest {

  @Test
  void familyMenuStartsFromActiveMerchantDishesAndDefaultsMissingConfiguration() throws Exception {
    String xml = Files.readString(
        Path.of("src/main/resources/mapper/family/FamilyMapper.xml"), StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ")
        .toLowerCase();

    assertTrue(xml.contains("from families f join dishes d on d.merchant_id = f.merchant_id"));
    assertTrue(xml.contains("left join family_menu_items fmi on fmi.family_id = f.id and fmi.dish_id = d.id"));
    assertTrue(xml.contains("d.merchant_id = #{merchantid}"));
    assertTrue(xml.contains("d.status = 'active'"));
    assertTrue(xml.contains("coalesce(fmi.final_price, d.base_price) as familyfinalprice"));
    assertTrue(xml.contains("coalesce(fmi.enabled, 0) as enabled"));
    assertTrue(xml.contains("d.featured_at as featuredat"));
    assertTrue(xml.contains("(d.featured_at is not null) as featured"));
    assertTrue(xml.contains("order by (d.featured_at is not null) desc, d.featured_at desc, "
        + "case when d.featured_at is null then coalesce(fmi.sort_order, 2147483647) end asc, "
        + "d.id desc"));
  }
}
