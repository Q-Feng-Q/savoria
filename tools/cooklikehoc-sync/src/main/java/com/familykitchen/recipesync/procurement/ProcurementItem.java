package com.familykitchen.recipesync.procurement;

import java.math.BigDecimal;

/** One leaf ingredient or one component occurrence in a procurement graph. */
public record ProcurementItem(
    String key,
    Kind kind,
    String ingredientName,
    QuantityStatus quantityStatus,
    BigDecimal quantity,
    String unit,
    String calcType,
    String sourceQuantityText,
    String componentKey,
    BigDecimal componentMultiplier) {

  public enum Kind {
    LEAF,
    COMPONENT
  }

  public enum QuantityStatus {
    VERIFIED,
    SOURCE_BATCH,
    MISSING,
    NOT_APPLICABLE
  }

  public static ProcurementItem verifiedLeaf(String key, String name, String quantity,
      String unit, String calcType) {
    return new ProcurementItem(key, Kind.LEAF, name, QuantityStatus.VERIFIED,
        new BigDecimal(quantity), unit, calcType, null, null, null);
  }

  public static ProcurementItem sourceBatch(String key, String name, String sourceQuantityText) {
    return new ProcurementItem(key, Kind.LEAF, name, QuantityStatus.SOURCE_BATCH,
        null, null, null, sourceQuantityText, null, null);
  }

  public static ProcurementItem missing(String key, String name) {
    return new ProcurementItem(key, Kind.LEAF, name, QuantityStatus.MISSING,
        null, null, null, null, null, null);
  }

  public static ProcurementItem notApplicable(String key, String name) {
    return new ProcurementItem(key, Kind.LEAF, name, QuantityStatus.NOT_APPLICABLE,
        null, null, null, null, null, null);
  }

  public static ProcurementItem component(String occurrenceKey, String componentKey,
      String multiplier) {
    return new ProcurementItem(occurrenceKey, Kind.COMPONENT, null, null, null, null, null,
        null, componentKey, multiplier == null ? null : new BigDecimal(multiplier));
  }
}
