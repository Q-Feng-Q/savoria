package com.familykitchen.recipesync;

import com.familykitchen.recipesync.config.SyncConfig;
import com.familykitchen.recipesync.config.SourceSyncSettings;
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
  }
}
