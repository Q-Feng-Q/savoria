package com.familykitchen.recipesync.rewrite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Blocks release when a high-similarity rewrite lacks a complete approved review. */
public final class RewriteReviewGate {

  /** Returns deterministic release blockers for review-required candidates. */
  public List<String> unresolved(List<StepReviewCandidate> candidates, double threshold,
      List<RewriteReview> reviews) {
    Map<String, RewriteReview> reviewByKey = new HashMap<>();
    for (RewriteReview review : reviews) {
      if (reviewByKey.putIfAbsent(review.reviewKey(), review) != null) {
        throw new IllegalArgumentException("Duplicate rewrite review: " + review.reviewKey());
      }
    }
    List<String> blockers = new ArrayList<>();
    candidates.stream()
        .filter(candidate -> candidate.similarityScore() >= threshold)
        .sorted(Comparator.comparing(StepReviewCandidate::sourceKey)
            .thenComparingInt(StepReviewCandidate::stepNo))
        .forEach(candidate -> {
          RewriteReview review = reviewByKey.get(candidate.reviewKey());
          if (review == null || review.reviewer() == null || review.reviewer().isBlank()
              || review.reviewedAt() == null) {
            blockers.add("MISSING_REWRITE_REVIEW:" + candidate.reviewKey());
          } else if (!"APPROVED".equals(review.disposition())) {
            blockers.add("REWRITE_REQUIRED:" + candidate.reviewKey());
          }
        });
    return List.copyOf(blockers);
  }
}
