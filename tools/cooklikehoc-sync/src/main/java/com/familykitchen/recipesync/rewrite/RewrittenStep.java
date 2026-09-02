package com.familykitchen.recipesync.rewrite;

/** Cooking step rewritten to factual project wording. */
public record RewrittenStep(
    int stepNo,
    String title,
    String content,
    Integer durationSeconds,
    String temperatureText,
    String heatLevel) {
}
