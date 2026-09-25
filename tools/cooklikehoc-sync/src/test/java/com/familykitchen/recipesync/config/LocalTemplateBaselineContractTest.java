package com.familykitchen.recipesync.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 固定菜谱同步所使用的本地模板基线。 */
class LocalTemplateBaselineContractTest {

  @Test
  void baselineKeepsTheReviewedLegacyIdentitySet() {
    LocalTemplateBaseline actual = LocalTemplateBaseline.load(
        Path.of("config/local-template-baseline.json").toAbsolutePath());

    assertEquals(240, actual.templates().size());
    assertEquals(403, actual.templates().stream().mapToInt(value -> value.ingredients().size()).sum());
    assertEquals("https://www.flickr.com/photos/10559879@N00/504366900",
        actual.templates().stream().filter(value -> value.id() == 199L).findFirst().orElseThrow()
            .imageSourceUrl());

    TemplateIdAllocations allocations = TemplateIdAllocations.load(
        Path.of("config/template-id-allocations.json").toAbsolutePath(), actual);
    assertEquals(552L, allocations.highWaterMark());
    assertEquals(552, allocations.allocations().size());
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
