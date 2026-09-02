package com.familykitchen.recipesync.config;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable command configuration for a recipe synchronization run. */
public record SyncConfig(
    Mode mode,
    Path sourceDir,
    Path configDir,
    Path outputDir,
    Path v4File,
    Path manifestFile,
    Path qualityReportFile) {

  /** Supported synchronization modes. */
  public enum Mode {
    DRAFT,
    RELEASE
  }

  /** Parses strict option/value command arguments. */
  public static SyncConfig parse(String[] args) {
    Map<String, String> values = parsePairs(args);
    Mode mode = parseMode(require(values, "--mode"));
    Path sourceDir = Path.of(require(values, "--source-dir"));
    Path configDir = Path.of(require(values, "--config-dir"));
    Path outputDir = Path.of(require(values, "--output-dir"));
    Path v4File = optionalPath(values, "--v4-file");
    Path manifestFile = optionalPath(values, "--manifest-file");
    Path qualityReportFile = optionalPath(values, "--quality-report-file");
    if (mode == Mode.RELEASE) {
      requirePath(v4File, "--v4-file");
      requirePath(manifestFile, "--manifest-file");
      requirePath(qualityReportFile, "--quality-report-file");
    }
    return new SyncConfig(mode, sourceDir, configDir, outputDir, v4File, manifestFile,
        qualityReportFile);
  }

  private static Map<String, String> parsePairs(String[] args) {
    if (args.length % 2 != 0) {
      throw new IllegalArgumentException("Every option must have a value");
    }
    Map<String, String> values = new LinkedHashMap<>();
    for (int index = 0; index < args.length; index += 2) {
      String option = args[index];
      if (!option.startsWith("--")) {
        throw new IllegalArgumentException("Invalid option: " + option);
      }
      if (values.putIfAbsent(option, args[index + 1]) != null) {
        throw new IllegalArgumentException("Duplicate argument: " + option);
      }
    }
    return values;
  }

  private static Mode parseMode(String value) {
    return switch (value) {
      case "draft" -> Mode.DRAFT;
      case "release" -> Mode.RELEASE;
      default -> throw new IllegalArgumentException("Invalid --mode: " + value);
    };
  }

  private static String require(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: " + key);
    }
    return value;
  }

  private static Path optionalPath(Map<String, String> values, String key) {
    String value = values.get(key);
    return value == null || value.isBlank() ? null : Path.of(value);
  }

  private static void requirePath(Path value, String key) {
    if (value == null) {
      throw new IllegalArgumentException("Missing required argument: " + key);
    }
  }
}
