package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.service.DishTemplateProcurementReadinessEvaluator;
import com.familykitchen.dish.service.impl.DishTemplateProcurementReadinessEvaluatorImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证运行时模板采购就绪判定与导入生成规则一致。 */
class DishTemplateProcurementReadinessEvaluatorTest {

  private final DishTemplateProcurementReadinessEvaluator evaluator =
      new DishTemplateProcurementReadinessEvaluatorImpl();

  @Test
  void verifiedLeafAndRepeatedComponentPathsAreReady() {
    DishTemplateEntity dish = template(1L, "DISH");
    DishTemplateEntity sauce = template(2L, "COMPONENT");
    DishTemplateIngredientEntity direct = verified(1L, "鸡肉", "500", "g");
    DishTemplateIngredientEntity first = component(1L, 2L, "first", "1");
    DishTemplateIngredientEntity second = component(1L, 2L, "second", "2");
    DishTemplateIngredientEntity componentLeaf = verified(2L, "酱油", "20", "ml");

    var result = evaluator.evaluate(1L, List.of(dish, sauce),
        List.of(direct, first, second, componentLeaf));

    assertTrue(result.ready());
    assertTrue(result.blockingReasons().isEmpty());
  }

  @Test
  void unknownQuantityAndNullComponentMultiplierBlockReadiness() {
    DishTemplateEntity dish = template(1L, "DISH");
    DishTemplateEntity sauce = template(2L, "COMPONENT");
    DishTemplateIngredientEntity missing = new DishTemplateIngredientEntity();
    missing.setTemplateId(1L);
    missing.setIngredientName("盐");
    missing.setQuantityStatus("MISSING");
    DishTemplateIngredientEntity component = component(1L, 2L, "sauce", null);

    var result = evaluator.evaluate(1L, List.of(dish, sauce), List.of(missing, component));

    assertFalse(result.ready());
    assertTrue(result.blockingReasons().stream().anyMatch(value -> value.contains("MISSING")));
    assertTrue(result.blockingReasons().stream().anyMatch(value -> value.contains("组件倍数")));
  }

  @Test
  void cycleAndIncompatibleUnitsBlockReadiness() {
    DishTemplateEntity dish = template(1L, "DISH");
    DishTemplateEntity component = template(2L, "COMPONENT");
    DishTemplateIngredientEntity toComponent = component(1L, 2L, "to-component", "1");
    DishTemplateIngredientEntity cycle = component(2L, 1L, "cycle", "1");
    DishTemplateIngredientEntity grams = verified(1L, "水", "100", "g");
    DishTemplateIngredientEntity milliliters = verified(1L, "水", "100", "ml");

    var result = evaluator.evaluate(1L, List.of(dish, component),
        List.of(toComponent, cycle, grams, milliliters));

    assertFalse(result.ready());
    assertTrue(result.blockingReasons().stream().anyMatch(value -> value.contains("循环")));
    assertTrue(result.blockingReasons().stream().anyMatch(value -> value.contains("单位不兼容")));
  }

  private static DishTemplateEntity template(Long id, String type) {
    DishTemplateEntity entity = new DishTemplateEntity();
    entity.setId(id);
    entity.setTemplateType(type);
    return entity;
  }

  private static DishTemplateIngredientEntity verified(Long templateId, String name,
      String quantity, String unit) {
    DishTemplateIngredientEntity entity = new DishTemplateIngredientEntity();
    entity.setTemplateId(templateId);
    entity.setIngredientName(name);
    entity.setQuantity(new BigDecimal(quantity));
    entity.setUnit(unit);
    entity.setCalcType("FIXED");
    entity.setQuantityStatus("VERIFIED");
    return entity;
  }

  private static DishTemplateIngredientEntity component(Long templateId, Long componentId,
      String occurrence, String multiplier) {
    DishTemplateIngredientEntity entity = new DishTemplateIngredientEntity();
    entity.setTemplateId(templateId);
    entity.setIngredientName("组件");
    entity.setQuantityStatus("NOT_APPLICABLE");
    entity.setComponentTemplateId(componentId);
    entity.setComponentOccurrenceKey(occurrence);
    if (multiplier != null) entity.setComponentMultiplier(new BigDecimal(multiplier));
    return entity;
  }
}
