package com.familykitchen.recipesync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

/** Versioned source settings loaded from {@code source-sync.json}. */
public record SourceSyncSettings(String sourceRevision) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Loads and validates source synchronization settings. */
  public static SourceSyncSettings load(Path configDir) {
    Path file = configDir.resolve("source-sync.json");
    try {
      SourceSyncSettings settings = OBJECT_MAPPER.readValue(file.toFile(), SourceSyncSettings.class);
      if (settings.sourceRevision() == null
          || !settings.sourceRevision().matches("[0-9a-f]{40}")) {
        throw new IllegalArgumentException("Invalid source revision in " + file);
      }
      return settings;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read source settings: " + file, exception);
    }
  }
}
