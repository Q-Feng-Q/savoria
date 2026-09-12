package com.familykitchen.recipesync.output;

/** Byte-ready deterministic synchronization artifacts. */
public record SyncArtifacts(
    String normalizedRecipesJson,
    String qualityReportJson,
    String generatedSql) {
}
