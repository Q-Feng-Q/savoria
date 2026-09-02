package com.familykitchen.recipesync.model;

import java.util.List;

/** Complete recipe facts parsed from one source Markdown file. */
public record SourceRecipe(
    String sourceKey,
    String title,
    String category,
    String imagePath,
    List<SourceIngredient> ingredients,
    List<SourceStep> steps,
    List<String> componentLinks,
    List<String> issues) {
}
