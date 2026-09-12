package com.familykitchen.recipesync.model;

/** Ordered cooking step extracted from the source recipe. */
public record SourceStep(
    int stepNo,
    String title,
    String content,
    Integer durationSeconds,
    String temperatureText,
    String heatLevel) {
}
