package com.familykitchen.recipesync.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Covers deterministic text and title normalization. */
class RecipeNormalizerTest {

  @Test
  void keepsVisibleLinkTextAndNormalizesBracketsAndWhitespace() {
    assertEquals("鱼香肉丝(家常版)",
        RecipeNormalizer.normalizeName("  [鱼香肉丝](./recipe.md)（家常版）  "));
  }

  @Test
  void composesUnicodeToNfc() {
    assertEquals("Café", RecipeNormalizer.normalizeName("Cafe\u0301"));
  }
}
