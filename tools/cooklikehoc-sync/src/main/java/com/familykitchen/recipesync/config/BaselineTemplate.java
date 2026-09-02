package com.familykitchen.recipesync.config;

import java.math.BigDecimal;
import java.util.List;

/** Complete local template snapshot extracted before migration consolidation. */
public record BaselineTemplate(
    long id,
    String templateCode,
    long categoryId,
    String name,
    String description,
    String imageUrl,
    String imageSourceUrl,
    String imageAuthor,
    String imageLicense,
    BigDecimal referencePrice,
    List<String> tasteTags,
    List<String> mealTags,
    int sortOrder,
    boolean enabled,
    List<BaselineIngredient> ingredients) {
}
