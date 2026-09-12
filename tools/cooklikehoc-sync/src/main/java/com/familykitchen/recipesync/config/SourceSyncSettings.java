package com.familykitchen.recipesync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Versioned source settings loaded from {@code source-sync.json}. */
public record SourceSyncSettings(
    String sourceRevision,
    int normalizationVersion,
    int tokenizerVersion,
    BigDecimal similarityThreshold,
    Map<String, Long> categoryMappings) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Set<String> SOURCE_CATEGORIES = Set.of(
      "主食", "凉拌", "卤菜", "早餐", "汤", "炒菜", "炖菜", "炸品", "烤类", "烫菜",
      "煮锅", "砂锅菜", "蒸菜", "配料", "饮品");

  /** Loads and validates source synchronization settings. */
  public static SourceSyncSettings load(Path configDir) {
    Path file = configDir.resolve("source-sync.json");
    try {
      SourceSyncSettings settings = OBJECT_MAPPER.readValue(file.toFile(), SourceSyncSettings.class);
      if (settings.sourceRevision() == null
          || !settings.sourceRevision().matches("[0-9a-f]{40}")) {
        throw new IllegalArgumentException("Invalid source revision in " + file);
      }
      if (settings.normalizationVersion() <= 0 || settings.tokenizerVersion() <= 0
          || settings.similarityThreshold() == null
          || settings.similarityThreshold().signum() <= 0
          || settings.similarityThreshold().compareTo(BigDecimal.ONE) >= 0
          || settings.categoryMappings() == null
          || !settings.categoryMappings().keySet().equals(SOURCE_CATEGORIES)
          || settings.categoryMappings().values().stream().anyMatch(value -> value == null || value <= 0)) {
        throw new IllegalArgumentException("Invalid versioned source settings in " + file);
      }
      return settings;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read source settings: " + file, exception);
    }
  }
}
