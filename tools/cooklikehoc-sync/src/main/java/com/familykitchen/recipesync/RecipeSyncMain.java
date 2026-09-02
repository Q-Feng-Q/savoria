package com.familykitchen.recipesync;

import com.familykitchen.recipesync.config.LocalTemplateBaseline;
import com.familykitchen.recipesync.config.SyncConfig;
import com.familykitchen.recipesync.config.SourceSyncSettings;
import com.familykitchen.recipesync.config.TemplateIdAllocations;
import com.familykitchen.recipesync.config.TemplateNameMappings;
import com.familykitchen.recipesync.source.SourceRevisionVerifier;

/** Command-line entry point for deterministic CookLikeHOC synchronization. */
public final class RecipeSyncMain {

  private RecipeSyncMain() {
  }

  /** Starts the synchronization command. */
  public static void main(String[] args) {
    SyncConfig config = SyncConfig.parse(args);
    SourceSyncSettings settings = SourceSyncSettings.load(config.configDir());
    new SourceRevisionVerifier().verify(config.sourceDir(), settings.sourceRevision());
    LocalTemplateBaseline baseline = LocalTemplateBaseline.load(
        config.configDir().resolve("local-template-baseline.json"));
    TemplateIdAllocations.load(config.configDir().resolve("template-id-allocations.json"), baseline);
    TemplateNameMappings.load(config.configDir().resolve("template-name-mappings.json"));
  }
}
