package com.familykitchen.recipesync.merge;

import java.math.BigDecimal;

/** Existing local template facts that survive a source-led merge where applicable. */
public record LocalTemplate(
    long id,
    String templateCode,
    String name,
    String description,
    BigDecimal referencePrice,
    String sourceKey) {
}
