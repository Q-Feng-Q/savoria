package com.familykitchen.recipesync.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers deterministic duplicate-primary and alias mapping validation. */
class TemplateNameMappingsTest {

  @Test
  void loadsThreeReviewedDuplicatePrimaryGroups() {
    TemplateNameMappings mappings = TemplateNameMappings.load(
        Path.of("config/template-name-mappings.json").toAbsolutePath());

    assertEquals(3, mappings.primarySourceByNormalizedTitle().size());
  }

  @Test
  void rejectsAliasesThatNormalizeToTheSameName(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("mappings.json");
    Files.writeString(file, """
        {
          "schemaVersion": 1,
          "explicitSourceMappings": {},
          "primarySourceByNormalizedTitle": {},
          "nameAliases": [
            {"aliasName":"鱼香肉丝（家常）","templateId":1},
            {"aliasName":"鱼香肉丝(家常)","templateId":2}
          ]
        }
        """, StandardCharsets.UTF_8);

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> TemplateNameMappings.load(file));

    assertEquals("Alias normalization collision: 鱼香肉丝(家常)", error.getMessage());
  }
}
