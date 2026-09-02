package com.familykitchen.recipesync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Versioned permanent identity allocations with baseline anti-renumbering checks. */
public record TemplateIdAllocations(
    int schemaVersion,
    long highWaterMark,
    List<TemplateIdAllocation> allocations) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Loads allocations and proves every pre-existing local identity is unchanged. */
  public static TemplateIdAllocations load(Path file, LocalTemplateBaseline baseline) {
    try {
      TemplateIdAllocations values = OBJECT_MAPPER.readValue(file.toFile(),
          TemplateIdAllocations.class);
      values.validate(baseline);
      return values;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read template allocations: " + file, exception);
    }
  }

  private void validate(LocalTemplateBaseline baseline) {
    if (schemaVersion != 1) {
      throw new IllegalArgumentException("Unsupported template allocation schema: " + schemaVersion);
    }
    Map<String, TemplateIdAllocation> byKey = new HashMap<>();
    Set<Long> ids = new HashSet<>();
    Set<String> codes = new HashSet<>();
    for (TemplateIdAllocation allocation : allocations) {
      if (allocation.stableKey() == null || allocation.stableKey().isBlank()
          || allocation.templateCode() == null || allocation.templateCode().isBlank()
          || allocation.id() <= 0 || allocation.id() > highWaterMark
          || byKey.putIfAbsent(allocation.stableKey(), allocation) != null
          || !ids.add(allocation.id()) || !codes.add(allocation.templateCode())) {
        throw new IllegalArgumentException("Invalid or colliding template allocation: " + allocation);
      }
    }
    for (BaselineTemplate template : baseline.templates()) {
      TemplateIdAllocation allocation = byKey.get("local:" + template.templateCode());
      if (allocation == null || allocation.id() != template.id()
          || !allocation.templateCode().equals(template.templateCode())) {
        throw new IllegalArgumentException(
            "Missing or renumbered local allocation: " + template.templateCode());
      }
    }
  }
}
