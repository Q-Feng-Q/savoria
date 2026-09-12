package com.familykitchen.recipesync.procurement;

import com.familykitchen.recipesync.normalize.RecipeNormalizer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Expands component occurrences and verifies all procurement leaves. */
public final class ProcurementReadinessEvaluator {

  private static final Set<String> SUPPORTED_CALC_TYPES = Set.of("FIXED", "PER_PERSON");

  /** Evaluates one root template without deduplicating component paths. */
  public ProcurementEvaluation evaluate(String rootKey, Map<String, ProcurementTemplate> graph) {
    Set<String> reasons = new LinkedHashSet<>();
    List<ExpandedLeaf> leaves = new ArrayList<>();
    expand(rootKey, BigDecimal.ONE, graph, new ArrayList<>(), reasons, leaves);
    List<AggregatedProcurementItem> aggregated = aggregate(leaves, reasons);
    List<String> sortedReasons = reasons.stream().sorted().toList();
    return new ProcurementEvaluation(sortedReasons.isEmpty(), sortedReasons, aggregated);
  }

  private void expand(String templateKey, BigDecimal multiplier,
      Map<String, ProcurementTemplate> graph, List<String> path, Set<String> reasons,
      List<ExpandedLeaf> leaves) {
    if (path.contains(templateKey)) {
      List<String> cycle = new ArrayList<>(path.subList(path.indexOf(templateKey), path.size()));
      cycle.add(templateKey);
      reasons.add("COMPONENT_CYCLE:" + String.join("->", cycle));
      return;
    }
    ProcurementTemplate template = graph.get(templateKey);
    if (template == null) {
      reasons.add("UNRESOLVED_COMPONENT:" + templateKey);
      return;
    }
    path.add(templateKey);
    for (ProcurementItem item : template.items()) {
      if (item.kind() == ProcurementItem.Kind.COMPONENT) {
        if (item.componentMultiplier() == null) {
          reasons.add("MISSING_COMPONENT_MULTIPLIER:" + item.key());
          continue;
        }
        if (item.componentMultiplier().signum() <= 0) {
          reasons.add("INVALID_COMPONENT_MULTIPLIER:" + item.key());
          continue;
        }
        expand(item.componentKey(), multiplier.multiply(item.componentMultiplier()), graph,
            path, reasons, leaves);
      } else {
        collectLeaf(item, multiplier, reasons, leaves);
      }
    }
    path.remove(path.size() - 1);
  }

  private void collectLeaf(ProcurementItem item, BigDecimal multiplier, Set<String> reasons,
      List<ExpandedLeaf> leaves) {
    if (item.quantityStatus() == ProcurementItem.QuantityStatus.NOT_APPLICABLE) {
      return;
    }
    if (item.quantityStatus() == ProcurementItem.QuantityStatus.SOURCE_BATCH) {
      reasons.add("SOURCE_BATCH:" + item.key());
      return;
    }
    if (item.quantityStatus() == ProcurementItem.QuantityStatus.MISSING) {
      reasons.add("MISSING_QUANTITY:" + item.key());
      return;
    }
    if (item.quantity() == null || item.quantity().signum() <= 0
        || item.unit() == null || item.unit().isBlank()
        || item.calcType() == null || !SUPPORTED_CALC_TYPES.contains(item.calcType())) {
      reasons.add("INVALID_VERIFIED_QUANTITY:" + item.key());
      return;
    }
    UnitValue unitValue = canonicalUnit(item.unit(), item.quantity().multiply(multiplier));
    leaves.add(new ExpandedLeaf(RecipeNormalizer.normalizeName(item.ingredientName()),
        unitValue.quantity(), unitValue.unit(), unitValue.dimension(), item.calcType()));
  }

  private List<AggregatedProcurementItem> aggregate(List<ExpandedLeaf> leaves,
      Set<String> reasons) {
    Map<AggregateKey, BigDecimal> quantities = new HashMap<>();
    Map<NameCalcKey, Set<String>> dimensions = new HashMap<>();
    for (ExpandedLeaf leaf : leaves) {
      AggregateKey key = new AggregateKey(leaf.name(), leaf.unit(), leaf.calcType());
      quantities.merge(key, leaf.quantity(), BigDecimal::add);
      dimensions.computeIfAbsent(new NameCalcKey(leaf.name(), leaf.calcType()), ignored -> new HashSet<>())
          .add(leaf.dimension());
    }
    for (Map.Entry<NameCalcKey, Set<String>> entry : dimensions.entrySet()) {
      if (entry.getValue().size() > 1) {
        reasons.add("INCOMPATIBLE_UNIT:" + entry.getKey().name());
      }
    }
    return quantities.entrySet().stream()
        .map(entry -> new AggregatedProcurementItem(entry.getKey().name(), entry.getValue(),
            entry.getKey().unit(), entry.getKey().calcType()))
        .sorted(Comparator.comparing(AggregatedProcurementItem::ingredientName)
            .thenComparing(AggregatedProcurementItem::unit)
            .thenComparing(AggregatedProcurementItem::calcType))
        .toList();
  }

  private UnitValue canonicalUnit(String rawUnit, BigDecimal quantity) {
    return switch (rawUnit.trim().toLowerCase()) {
      case "kg", "千克" -> new UnitValue(quantity.multiply(new BigDecimal("1000")), "g", "MASS");
      case "g", "克" -> new UnitValue(quantity, "g", "MASS");
      case "l", "升" -> new UnitValue(quantity.multiply(new BigDecimal("1000")), "ml", "VOLUME");
      case "ml", "毫升" -> new UnitValue(quantity, "ml", "VOLUME");
      default -> new UnitValue(quantity, rawUnit.trim(), "COUNT:" + rawUnit.trim());
    };
  }

  private record ExpandedLeaf(
      String name,
      BigDecimal quantity,
      String unit,
      String dimension,
      String calcType) {
  }

  private record UnitValue(BigDecimal quantity, String unit, String dimension) {
  }

  private record AggregateKey(String name, String unit, String calcType) {
  }

  private record NameCalcKey(String name, String calcType) {
  }
}
