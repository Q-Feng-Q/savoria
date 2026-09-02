package com.familykitchen.recipesync.config;

import com.familykitchen.recipesync.normalize.RecipeNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reviewed source matching, duplicate-primary and local alias configuration. */
public record TemplateNameMappings(
    int schemaVersion,
    Map<String, Long> explicitSourceMappings,
    Map<String, String> primarySourceByNormalizedTitle,
    List<NameAliasMapping> nameAliases) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Loads and validates deterministic name mappings. */
  public static TemplateNameMappings load(Path file) {
    try {
      TemplateNameMappings mappings = OBJECT_MAPPER.readValue(file.toFile(),
          TemplateNameMappings.class);
      mappings.validate();
      return mappings;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read template name mappings: " + file,
          exception);
    }
  }

  /** Builds the normalized alias index consumed by the merge planner. */
  public Map<String, Long> normalizedAliases() {
    Map<String, Long> result = new LinkedHashMap<>();
    for (NameAliasMapping alias : nameAliases) {
      result.put(RecipeNormalizer.normalizeName(alias.aliasName()), alias.templateId());
    }
    return Map.copyOf(result);
  }

  private void validate() {
    if (schemaVersion != 1 || explicitSourceMappings == null
        || primarySourceByNormalizedTitle == null || nameAliases == null) {
      throw new IllegalArgumentException("Invalid template name mapping document");
    }
    for (Map.Entry<String, String> entry : primarySourceByNormalizedTitle.entrySet()) {
      if (!entry.getKey().equals(RecipeNormalizer.normalizeName(entry.getKey()))
          || entry.getValue() == null || entry.getValue().isBlank()) {
        throw new IllegalArgumentException("Invalid duplicate primary mapping: " + entry);
      }
    }
    Map<String, Long> normalized = new LinkedHashMap<>();
    for (NameAliasMapping alias : nameAliases) {
      String name = RecipeNormalizer.normalizeName(alias.aliasName());
      if (name == null || name.isBlank() || alias.templateId() <= 0
          || normalized.putIfAbsent(name, alias.templateId()) != null) {
        throw new IllegalArgumentException("Alias normalization collision: " + name);
      }
    }
  }
}
