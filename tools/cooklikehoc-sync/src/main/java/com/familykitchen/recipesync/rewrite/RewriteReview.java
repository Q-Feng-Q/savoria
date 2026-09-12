package com.familykitchen.recipesync.rewrite;

import java.time.Instant;

/** Versioned human disposition for one high-similarity rewritten step. */
public record RewriteReview(
    String sourceKey,
    int stepNo,
    String reviewer,
    Instant reviewedAt,
    String disposition,
    String correctedContent) {

  public String reviewKey() {
    return sourceKey + "#" + stepNo;
  }
}
