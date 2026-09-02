package com.familykitchen.recipesync.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Locks the versioned JSON baseline to the migrations that will be consolidated. */
class LocalTemplateBaselineContractTest {

  private final Path repositoryRoot = Path.of("../..").toAbsolutePath().normalize();
  private final List<Path> legacyMigrations = List.of(
      repositoryRoot.resolve("backend/src/main/resources/db/migration/V4__init_dish_template_market.sql"),
      repositoryRoot.resolve("backend/src/main/resources/db/migration/V5__expand_regional_dish_templates.sql"),
      repositoryRoot.resolve("backend/src/main/resources/db/migration/V6__finalize_regional_dish_template_images.sql"));

  @Test
  void baselineMatchesAllPreConsolidationTemplateAndIngredientRows() {
    LocalTemplateBaseline expected = LegacyMigrationBaselineExtractor.extract(legacyMigrations);
    LocalTemplateBaseline actual = LocalTemplateBaseline.load(
        Path.of("config/local-template-baseline.json").toAbsolutePath());

    assertEquals(240, actual.templates().size());
    assertEquals(403, actual.templates().stream().mapToInt(value -> value.ingredients().size()).sum());
    assertEquals(expected, actual);
    assertEquals("https://www.flickr.com/photos/10559879@N00/504366900",
        actual.templates().stream().filter(value -> value.id() == 199L).findFirst().orElseThrow()
            .imageSourceUrl());

    TemplateIdAllocations allocations = TemplateIdAllocations.load(
        Path.of("config/template-id-allocations.json").toAbsolutePath(), actual);
    assertEquals(240L, allocations.highWaterMark());
    assertEquals(240, allocations.allocations().size());
  }

  @Test
  void sourceSettingsVersionAllNormalizationInputsAndFifteenCategories() {
    SourceSyncSettings settings = SourceSyncSettings.load(Path.of("config").toAbsolutePath());

    assertEquals(1, settings.normalizationVersion());
    assertEquals(1, settings.tokenizerVersion());
    assertEquals(15, settings.categoryMappings().size());
    assertEquals(10L, settings.categoryMappings().get("配料"));
  }
}
