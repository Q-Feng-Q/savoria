package com.familykitchen.recipesync.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Covers procurement graph expansion and blocking rules. */
class ProcurementReadinessEvaluatorTest {

  private final ProcurementReadinessEvaluator evaluator = new ProcurementReadinessEvaluator();

  @Test
  void repeatedComponentPathsKeepBothMultipliers() {
    ProcurementTemplate sauce = new ProcurementTemplate("component/sauce", List.of(
        ProcurementItem.verifiedLeaf("soy", "酱油", "10", "ml", "FIXED")));
    ProcurementTemplate dish = new ProcurementTemplate("dish/a", List.of(
        ProcurementItem.component("sauce-1", "component/sauce", "2"),
        ProcurementItem.component("sauce-2", "component/sauce", "1")));

    ProcurementEvaluation result = evaluator.evaluate("dish/a", Map.of(
        dish.key(), dish,
        sauce.key(), sauce));

    assertTrue(result.ready());
    assertEquals(new BigDecimal("30"), result.aggregatedItems().get(0).quantity());
  }

  @Test
  void nullComponentMultiplierBlocksProcurement() {
    ProcurementTemplate component = new ProcurementTemplate("component/a", List.of(
        ProcurementItem.verifiedLeaf("salt", "盐", "2", "g", "FIXED")));
    ProcurementTemplate dish = new ProcurementTemplate("dish/a", List.of(
        ProcurementItem.component("component-use-1", "component/a", null)));

    ProcurementEvaluation result = evaluator.evaluate("dish/a", Map.of(
        dish.key(), dish,
        component.key(), component));

    assertFalse(result.ready());
    assertTrue(result.blockingReasons().contains("MISSING_COMPONENT_MULTIPLIER:component-use-1"));
  }

  @Test
  void sourceBatchCycleAndUnresolvedComponentsBlockProcurement() {
    ProcurementTemplate first = new ProcurementTemplate("component/first", List.of(
        ProcurementItem.component("to-second", "component/second", "1")));
    ProcurementTemplate second = new ProcurementTemplate("component/second", List.of(
        ProcurementItem.component("to-first", "component/first", "1")));
    ProcurementTemplate dish = new ProcurementTemplate("dish/a", List.of(
        ProcurementItem.sourceBatch("batch", "底料", "500g"),
        ProcurementItem.component("cycle", "component/first", "1"),
        ProcurementItem.component("missing", "component/missing", "1")));

    ProcurementEvaluation result = evaluator.evaluate("dish/a", Map.of(
        dish.key(), dish,
        first.key(), first,
        second.key(), second));

    assertFalse(result.ready());
    assertTrue(result.blockingReasons().stream().anyMatch(value -> value.startsWith("SOURCE_BATCH:")));
    assertTrue(result.blockingReasons().stream().anyMatch(value -> value.startsWith("COMPONENT_CYCLE:")));
    assertTrue(result.blockingReasons().contains("UNRESOLVED_COMPONENT:component/missing"));
  }

  @Test
  void incompatibleUnitsForSameIngredientAreNotSummed() {
    ProcurementTemplate dish = new ProcurementTemplate("dish/a", List.of(
        ProcurementItem.verifiedLeaf("oil-1", "食用油", "10", "ml", "FIXED"),
        ProcurementItem.verifiedLeaf("oil-2", "食用油", "5", "g", "FIXED")));

    ProcurementEvaluation result = evaluator.evaluate("dish/a", Map.of(dish.key(), dish));

    assertFalse(result.ready());
    assertEquals(2, result.aggregatedItems().size());
    assertTrue(result.blockingReasons().contains("INCOMPATIBLE_UNIT:食用油"));
  }
}
