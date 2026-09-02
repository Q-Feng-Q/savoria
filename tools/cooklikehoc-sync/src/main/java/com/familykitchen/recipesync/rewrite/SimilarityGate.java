package com.familykitchen.recipesync.rewrite;

import java.util.LinkedHashSet;
import java.util.Set;

/** Deterministic character-bigram similarity used only to select manual reviews. */
public final class SimilarityGate {

  /** Returns a Jaccard score in the inclusive range 0 to 1. */
  public double score(String source, String rewritten) {
    Set<String> sourceTokens = bigrams(normalize(source));
    Set<String> rewrittenTokens = bigrams(normalize(rewritten));
    if (sourceTokens.isEmpty() && rewrittenTokens.isEmpty()) {
      return 1.0;
    }
    Set<String> intersection = new LinkedHashSet<>(sourceTokens);
    intersection.retainAll(rewrittenTokens);
    Set<String> union = new LinkedHashSet<>(sourceTokens);
    union.addAll(rewrittenTokens);
    return (double) intersection.size() / union.size();
  }

  private String normalize(String value) {
    return value == null ? "" : value.replaceAll("[\\s，。；：、,.!！?？]", "");
  }

  private Set<String> bigrams(String value) {
    Set<String> result = new LinkedHashSet<>();
    if (value.length() == 1) {
      result.add(value);
    }
    for (int index = 0; index + 1 < value.length(); index++) {
      result.add(value.substring(index, index + 2));
    }
    return result;
  }
}
