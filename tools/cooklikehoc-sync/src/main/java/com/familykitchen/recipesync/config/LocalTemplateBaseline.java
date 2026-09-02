package com.familykitchen.recipesync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Sole machine-readable baseline for all templates that predate source synchronization. */
public record LocalTemplateBaseline(
    int schemaVersion,
    List<String> sourceMigrations,
    List<BaselineTemplate> templates) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  /** Loads the committed UTF-8 baseline. */
  public static LocalTemplateBaseline load(Path file) {
    try {
      return OBJECT_MAPPER.readValue(file.toFile(), LocalTemplateBaseline.class);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read local template baseline: " + file, exception);
    }
  }

  /** Writes a stable UTF-8 JSON representation used only to bootstrap the committed baseline. */
  public void write(Path file) {
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      String json = OBJECT_MAPPER.writeValueAsString(this).replace("\r\n", "\n") + "\n";
      Files.writeString(file, json, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to write local template baseline: " + file, exception);
    }
  }
}
