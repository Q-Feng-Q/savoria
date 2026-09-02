package com.familykitchen.recipesync.config;

/** One immutable stable-key to database-identity allocation. */
public record TemplateIdAllocation(String stableKey, long id, String templateCode) {
}
