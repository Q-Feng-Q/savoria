package com.familykitchen.recipesync.parser;

import java.text.Normalizer;
import java.util.Locale;

/** Maps known source section headings to parser responsibilities. */
final class RecipeSectionClassifier {

  enum Section {
    NONE,
    INGREDIENTS,
    STEPS
  }

  Section classify(String heading) {
    String value = Normalizer.normalize(heading, Normalizer.Form.NFC)
        .replaceAll("[：:]$", "")
        .trim()
        .toLowerCase(Locale.ROOT);
    if (containsAny(value, "原料", "食材", "已知成分", "必备原料", "配料")) {
      return Section.INGREDIENTS;
    }
    if (containsAny(value, "步骤", "制作流程", "做法", "计算")) {
      return Section.STEPS;
    }
    return Section.NONE;
  }

  private boolean containsAny(String value, String... candidates) {
    for (String candidate : candidates) {
      if (value.contains(candidate)) {
        return true;
      }
    }
    return false;
  }
}
