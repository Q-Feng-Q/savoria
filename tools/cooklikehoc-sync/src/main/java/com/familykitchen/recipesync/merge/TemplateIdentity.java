package com.familykitchen.recipesync.merge;

/** Permanently allocated database identity for a stable template key. */
public record TemplateIdentity(long id, String templateCode) {
}
