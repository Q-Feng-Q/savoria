package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies merchant recommendations and the deterministic home fallback SQL. */
class FamilyFeaturedDishMapperContractTest {
  @Test
  void homeExposesUpToFiveActiveMerchantRecommendationsThenOffersDeterministicFallback() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/mapper/family/FamilyMapper.xml"), StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ").toLowerCase();
    assertTrue(xml.contains("<select id=\"selectfeatureddishes\""));
    assertTrue(xml.contains("from families f join family_menu_items fmi on fmi.family_id = f.id"));
    assertTrue(xml.contains("join dishes d on d.id = fmi.dish_id and d.merchant_id = f.merchant_id"));
    assertTrue(xml.contains("f.status = 'active'"));
    assertTrue(xml.contains("fmi.enabled = 1"));
    assertTrue(xml.contains("d.status = 'active'"));
    assertTrue(xml.contains("d.featured_at is not null"));
    assertTrue(xml.contains("order by d.featured_at desc, d.id desc"));
    assertTrue(xml.contains("<bind name=\"featurelimit\" value=\"@java.lang.math@min(@java.lang.math@max(limit, 0), 5)\"/>"));
    assertTrue(xml.contains("limit #{featurelimit}"));
    assertTrue(xml.contains("select d.id as dishid, d.name, d.description, coalesce(fmi.final_price, d.base_price) as price, d.image_url as imageurl"));

    assertTrue(xml.contains("<select id=\"selectfallbackdish\""));
    assertTrue(xml.contains("order by fmi.sort_order asc, d.id desc"));
    assertTrue(xml.contains("d.description"));
  }

  @Test
  void compatibilityQueryReturnsRecommendationBeforeFallback() {
    FamilyHomeResponse.FeaturedDish recommended = dish(11L, "recommended");
    AtomicInteger fallbackCalls = new AtomicInteger();
    FamilyMapper mapper = mapper(List.of(recommended), dish(12L, "fallback"), fallbackCalls);

    assertSame(recommended, mapper.selectFeaturedDish(7L));
    assertTrue(fallbackCalls.get() == 0);
  }

  @Test
  void compatibilityQueryUsesFallbackOnlyWhenThereIsNoRecommendation() {
    FamilyHomeResponse.FeaturedDish fallback = dish(12L, "fallback");
    AtomicInteger fallbackCalls = new AtomicInteger();
    FamilyMapper mapper = mapper(List.of(), fallback, fallbackCalls);

    assertSame(fallback, mapper.selectFeaturedDish(7L));
    assertTrue(fallbackCalls.get() == 1);
  }

  private static FamilyMapper mapper(List<FamilyHomeResponse.FeaturedDish> recommendations,
      FamilyHomeResponse.FeaturedDish fallback, AtomicInteger fallbackCalls) {
    return (FamilyMapper) Proxy.newProxyInstance(
        FamilyMapper.class.getClassLoader(),
        new Class<?>[] {FamilyMapper.class},
        (proxy, method, args) -> {
          if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
          }
          return switch (method.getName()) {
            case "selectFeaturedDishes" -> recommendations;
            case "selectFallbackDish" -> {
              fallbackCalls.incrementAndGet();
              yield fallback;
            }
            default -> throw new AssertionError("Unexpected mapper method: " + method.getName());
          };
        });
  }

  private static FamilyHomeResponse.FeaturedDish dish(Long id, String name) {
    return new FamilyHomeResponse.FeaturedDish(id, name, "description", BigDecimal.ONE, "image");
  }
}
