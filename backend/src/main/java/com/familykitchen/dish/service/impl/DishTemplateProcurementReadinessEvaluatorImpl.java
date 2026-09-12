package com.familykitchen.dish.service.impl;

import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.service.DishTemplateProcurementReadinessEvaluator;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 基于模板组件图和数量状态实现统一采购就绪判定。 */
@Service
public class DishTemplateProcurementReadinessEvaluatorImpl
    implements DishTemplateProcurementReadinessEvaluator {

  /** {@inheritDoc} */
  @Override
  public EvaluationResult evaluate(Long rootTemplateId, List<DishTemplateEntity> templates,
      List<DishTemplateIngredientEntity> ingredients) {
    Map<Long, DishTemplateEntity> templateById = new HashMap<>();
    templates.forEach(template -> templateById.put(template.getId(), template));
    Map<Long, List<DishTemplateIngredientEntity>> ingredientByTemplate = new HashMap<>();
    ingredients.forEach(ingredient -> ingredientByTemplate
        .computeIfAbsent(ingredient.getTemplateId(), ignored -> new ArrayList<>()).add(ingredient));

    LinkedHashSet<String> reasons = new LinkedHashSet<>();
    List<Leaf> leaves = new ArrayList<>();
    if (!templateById.containsKey(rootTemplateId)) {
      reasons.add("根模板不存在：" + rootTemplateId);
    } else {
      visit(rootTemplateId, BigDecimal.ONE, templateById, ingredientByTemplate,
          new ArrayDeque<>(), reasons, leaves);
      validateCompatibleUnits(leaves, reasons);
    }
    if (reasons.isEmpty() && leaves.isEmpty()) reasons.add("成品模板没有可采购的叶子食材");
    List<ProcurementItem> items = reasons.isEmpty() ? aggregate(leaves) : List.of();
    return new EvaluationResult(reasons.isEmpty(), List.copyOf(reasons), items);
  }

  private void visit(Long templateId, BigDecimal pathMultiplier,
      Map<Long, DishTemplateEntity> templateById,
      Map<Long, List<DishTemplateIngredientEntity>> ingredientByTemplate,
      ArrayDeque<Long> path, Set<String> reasons, List<Leaf> leaves) {
    if (path.contains(templateId)) {
      reasons.add("组件引用存在循环：" + renderCycle(path, templateId));
      return;
    }
    if (!templateById.containsKey(templateId)) {
      reasons.add("组件模板未解析：" + templateId);
      return;
    }

    path.addLast(templateId);
    for (DishTemplateIngredientEntity ingredient
        : ingredientByTemplate.getOrDefault(templateId, List.of())) {
      if (ingredient.getComponentTemplateId() != null) {
        BigDecimal multiplier = ingredient.getComponentMultiplier();
        if (multiplier == null || multiplier.signum() <= 0) {
          reasons.add("组件倍数缺失或无效：" + display(ingredient));
          continue;
        }
        visit(ingredient.getComponentTemplateId(), pathMultiplier.multiply(multiplier),
            templateById, ingredientByTemplate, path, reasons, leaves);
        continue;
      }
      validateLeaf(ingredient, pathMultiplier, reasons, leaves);
    }
    path.removeLast();
  }

  private void validateLeaf(DishTemplateIngredientEntity ingredient, BigDecimal pathMultiplier,
      Set<String> reasons, List<Leaf> leaves) {
    String status = ingredient.getQuantityStatus();
    if ("NOT_APPLICABLE".equals(status)) return;
    if (!"VERIFIED".equals(status)) {
      reasons.add("食材数量状态为 " + (status == null ? "MISSING" : status) + "：" + display(ingredient));
      return;
    }
    if (ingredient.getQuantity() == null || ingredient.getQuantity().signum() <= 0
        || isBlank(ingredient.getUnit())
        || !("FIXED".equals(ingredient.getCalcType())
            || "PER_PERSON".equals(ingredient.getCalcType()))) {
      reasons.add("已核验食材的数量、单位或计算方式无效：" + display(ingredient));
      return;
    }
    Unit normalized = normalizeUnit(ingredient.getUnit());
    leaves.add(new Leaf(ingredient.getIngredientName(), normalizeName(ingredient.getIngredientName()),
        ingredient.getIngredientCategory(), normalized.family(), normalized.baseUnit(),
        ingredient.getCalcType(), ingredient.getQuantity().multiply(pathMultiplier)
            .multiply(normalized.factor())));
  }

  private List<ProcurementItem> aggregate(List<Leaf> leaves) {
    Map<String, ProcurementItem> aggregated = new LinkedHashMap<>();
    for (Leaf leaf : leaves) {
      String key = leaf.normalizedName() + "|" + leaf.unitFamily() + "|" + leaf.calcType();
      ProcurementItem previous = aggregated.get(key);
      BigDecimal quantity = previous == null ? leaf.quantity()
          : previous.quantity().add(leaf.quantity());
      aggregated.put(key, new ProcurementItem(
          previous == null ? leaf.displayName() : previous.name(),
          previous == null ? leaf.category() : previous.category(), quantity,
          leaf.baseUnit(), leaf.calcType()));
    }
    return List.copyOf(aggregated.values());
  }

  private void validateCompatibleUnits(List<Leaf> leaves, Set<String> reasons) {
    Map<String, Set<String>> familiesByIngredient = new HashMap<>();
    for (Leaf leaf : leaves) {
      String key = leaf.normalizedName() + "|" + leaf.calcType();
      familiesByIngredient.computeIfAbsent(key, ignored -> new HashSet<>()).add(leaf.unitFamily());
    }
    familiesByIngredient.forEach((key, families) -> {
      if (families.size() > 1) reasons.add("同名食材单位不兼容：" + key.substring(0, key.indexOf('|')));
    });
  }

  private static Unit normalizeUnit(String unit) {
    return switch (unit.trim().toLowerCase(Locale.ROOT)) {
      case "kg", "千克", "公斤" -> new Unit("MASS", "g", new BigDecimal("1000"));
      case "g", "克" -> new Unit("MASS", "g", BigDecimal.ONE);
      case "l", "升" -> new Unit("VOLUME", "ml", new BigDecimal("1000"));
      case "ml", "毫升" -> new Unit("VOLUME", "ml", BigDecimal.ONE);
      default -> new Unit("UNIT:" + unit.trim(), unit.trim(), BigDecimal.ONE);
    };
  }

  private static String renderCycle(ArrayDeque<Long> path, Long repeated) {
    return path + " -> " + repeated;
  }

  private static String display(DishTemplateIngredientEntity ingredient) {
    if (!isBlank(ingredient.getSourceLineKey())) return ingredient.getSourceLineKey();
    return isBlank(ingredient.getIngredientName()) ? "未命名食材" : ingredient.getIngredientName();
  }

  private static String normalizeName(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * 规范化计量单位族和换算到基础单位的倍数。
   * @param family 单位族
   * @param baseUnit 单位族的基础单位
   * @param factor 换算到基础单位的倍数
   */
  private record Unit(String family, String baseUnit, BigDecimal factor) { }

  /**
   * 展开组件路径后的采购叶子食材。
   * @param displayName 食材显示名称
   * @param normalizedName 规范化食材名称
   * @param category 食材分类
   * @param unitFamily 规范化单位族
   * @param baseUnit 规范化基础单位
   * @param calcType 采购计算方式
   * @param quantity 乘入完整组件路径倍数后的基础单位数量
   */
  private record Leaf(String displayName, String normalizedName, String category, String unitFamily,
                      String baseUnit, String calcType, BigDecimal quantity) { }
}
