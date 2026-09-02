package com.familykitchen.recipesync.rewrite;

/** Step whose source-to-rewrite similarity may require a reviewed disposition. */
public record StepReviewCandidate(String sourceKey, int stepNo, double similarityScore) {

  public String reviewKey() {
    return sourceKey + "#" + stepNo;
  }
}
