package com.familykitchen.recipesync.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.familykitchen.recipesync.model.SourceRecipe;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Covers stable, source-led template matching. */
class TemplateMergePlannerTest {

  private final TemplateMergePlanner planner = new TemplateMergePlanner();

  @Test
  void keepsLocalIdentityAndCommercialFieldsWhenSourceTitleMatches() {
    LocalTemplate local = local(12L, "DISH_012", "番茄炒蛋", "家常介绍", "18.00", null);
    SourceRecipe source = source("炒菜/西红柿炒鸡蛋.md", "西红柿炒鸡蛋");
    MergeInputs inputs = new MergeInputs(
        List.of(source), List.of(local),
        Map.of("炒菜/西红柿炒鸡蛋.md", 12L), Map.of(), Map.of(), Map.of(), 240L);

    MergedTemplate result = planner.plan(inputs).get(0);

    assertEquals(12L, result.id());
    assertEquals("DISH_012", result.templateCode());
    assertEquals("西红柿炒鸡蛋", result.name());
    assertEquals("家常介绍", result.description());
    assertEquals(new BigDecimal("18.00"), result.referencePrice());
    assertEquals(List.of("炒菜/西红柿炒鸡蛋.md"), result.sourceKeys());
  }

  @Test
  void requiresExplicitPrimaryForDuplicateNormalizedTitles() {
    MergeInputs inputs = new MergeInputs(
        List.of(source("炒菜/同名.md", "同名菜"), source("炖菜/同名.md", "同名菜")),
        List.of(), Map.of(), Map.of(), Map.of(), Map.of(), 240L);

    MergeFailure error = assertThrows(MergeFailure.class, () -> planner.plan(inputs));

    assertEquals("DUPLICATE_SOURCE_TITLE", error.code());
  }

  @Test
  void groupsDuplicateSourcesUnderConfiguredPrimaryAndUsesStableAllocation() {
    String primary = "炒菜/同名.md";
    MergeInputs inputs = new MergeInputs(
        List.of(source(primary, "同名菜"), source("炖菜/同名.md", "同名菜")),
        List.of(), Map.of(), Map.of(), Map.of("同名菜", primary),
        Map.of(primary, new TemplateIdentity(241L, "DISH_241")), 241L);

    List<MergedTemplate> results = planner.plan(inputs);

    assertEquals(1, results.size());
    assertEquals(241L, results.get(0).id());
    assertEquals(List.of(primary, "炖菜/同名.md"), results.get(0).sourceKeys());
  }

  @Test
  void rejectsRenumberedOrCollidingAllocations() {
    String first = "炒菜/甲.md";
    String second = "炒菜/乙.md";
    MergeInputs inputs = new MergeInputs(
        List.of(source(first, "甲"), source(second, "乙")), List.of(), Map.of(), Map.of(), Map.of(),
        Map.of(
            first, new TemplateIdentity(241L, "DISH_241"),
            second, new TemplateIdentity(241L, "DISH_242")),
        242L);

    MergeFailure error = assertThrows(MergeFailure.class, () -> planner.plan(inputs));

    assertEquals("ALLOCATION_COLLISION", error.code());
  }

  @Test
  void retainsUnmatchedLocalTemplatesAsExtensions() {
    LocalTemplate local = local(8L, "DISH_008", "私房菜", "保留", "25.00", null);
    MergeInputs inputs = new MergeInputs(
        List.of(), List.of(local), Map.of(), Map.of(), Map.of(), Map.of(), 240L);

    MergedTemplate result = planner.plan(inputs).get(0);

    assertEquals("LOCAL_EXTENSION", result.sourceType());
    assertEquals("私房菜", result.name());
  }

  private SourceRecipe source(String key, String title) {
    return new SourceRecipe(key, title, key.substring(0, key.indexOf('/')), null,
        List.of(), List.of(), List.of(), List.of());
  }

  private LocalTemplate local(long id, String code, String name, String description,
      String price, String sourceKey) {
    return new LocalTemplate(id, code, name, description, new BigDecimal(price), sourceKey);
  }
}
