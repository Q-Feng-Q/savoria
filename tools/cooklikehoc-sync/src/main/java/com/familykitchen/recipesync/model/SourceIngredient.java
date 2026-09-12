package com.familykitchen.recipesync.model;

/** Ingredient fact extracted from one source ingredient line. */
public record SourceIngredient(
    String sourceLineKey,
    String name,
    String sourceText,
    String sourceQuantityText) {
}
