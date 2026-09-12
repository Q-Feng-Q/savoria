package com.familykitchen.recipesync.merge;

import com.familykitchen.recipesync.model.SourceRecipe;
import java.math.BigDecimal;
import java.util.List;

/** Result of matching one primary source group or retaining one local extension. */
public record MergedTemplate(
    long id,
    String templateCode,
    String name,
    String description,
    BigDecimal referencePrice,
    String sourceType,
    List<String> sourceKeys,
    SourceRecipe primarySource) {
}
