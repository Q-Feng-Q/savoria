package com.familykitchen.recipesync.merge;

import com.familykitchen.recipesync.model.SourceRecipe;
import java.util.List;
import java.util.Map;

/** Complete versioned inputs for one deterministic merge plan. */
public record MergeInputs(
    List<SourceRecipe> sourceRecipes,
    List<LocalTemplate> localTemplates,
    Map<String, Long> explicitSourceMappings,
    Map<String, Long> normalizedNameAliases,
    Map<String, String> primarySourceByNormalizedTitle,
    Map<String, TemplateIdentity> allocations,
    long allocationHighWaterMark) {
}
