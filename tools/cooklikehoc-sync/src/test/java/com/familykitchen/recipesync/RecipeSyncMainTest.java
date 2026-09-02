package com.familykitchen.recipesync;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies that the recipe synchronization command exposes a stable entry point. */
class RecipeSyncMainTest {

  @Test
  void exposesExecutableMainClass() {
    assertDoesNotThrow(() -> Class.forName("com.familykitchen.recipesync.RecipeSyncMain"));
  }

  @Test
  void rejectsMissingRequiredArguments() {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> RecipeSyncMain.main(new String[] {"--mode", "draft"}));

    assertEquals("Missing required argument: --source-dir", error.getMessage());
  }

  @Test
  void rejectsCheckoutAtUnexpectedRevision(@TempDir Path tempDir) throws Exception {
    Path configDir = Files.createDirectories(tempDir.resolve("config"));
    Files.writeString(configDir.resolve("source-sync.json"),
        "{\"sourceRevision\":\"0000000000000000000000000000000000000000\"}",
        StandardCharsets.UTF_8);

    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> RecipeSyncMain.main(new String[] {
            "--mode", "draft",
            "--source-dir", Path.of("../..").toAbsolutePath().normalize().toString(),
            "--config-dir", configDir.toString(),
            "--output-dir", tempDir.resolve("output").toString()
        }));

    assertEquals("SOURCE_REVISION_MISMATCH", error.getMessage());
  }
}
