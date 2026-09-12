package com.familykitchen.recipesync.output;

/** Private source image metadata persisted for controlled platform review. */
public record PrivateImageAsset(
    String internalStorageKey,
    String contentSha256,
    String mimeType,
    long fileSize,
    String sourceImagePath,
    String sourceRevision,
    String assetStatus) {
}
