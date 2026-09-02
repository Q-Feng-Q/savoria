package com.familykitchen.recipesync.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers bounded and atomic V4 generated-section replacement. */
class V4GeneratedSectionUpdaterTest {

  @Test
  void replacesOnlyTheUniqueGeneratedSection(@TempDir Path tempDir) throws Exception {
    Path v4 = tempDir.resolve("V4.sql");
    Files.writeString(v4, """
        -- schema before
        -- BEGIN GENERATED COOKLIKEHOC DATA
        stale row;
        -- END GENERATED COOKLIKEHOC DATA
        -- schema after
        """, StandardCharsets.UTF_8);

    new V4GeneratedSectionUpdater().update(v4, "fresh row;\n");

    String result = Files.readString(v4, StandardCharsets.UTF_8);
    assertEquals("""
        -- schema before
        -- BEGIN GENERATED COOKLIKEHOC DATA
        fresh row;
        -- END GENERATED COOKLIKEHOC DATA
        -- schema after
        """, result);
    assertFalse(result.contains("stale row"));
  }

  @Test
  void duplicateMarkersFailWithoutChangingTheFile(@TempDir Path tempDir) throws Exception {
    Path v4 = tempDir.resolve("V4.sql");
    String invalid = """
        -- BEGIN GENERATED COOKLIKEHOC DATA
        -- END GENERATED COOKLIKEHOC DATA
        -- BEGIN GENERATED COOKLIKEHOC DATA
        -- END GENERATED COOKLIKEHOC DATA
        """;
    Files.writeString(v4, invalid, StandardCharsets.UTF_8);

    assertThrows(IllegalArgumentException.class,
        () -> new V4GeneratedSectionUpdater().update(v4, "new row;\n"));

    assertEquals(invalid, Files.readString(v4, StandardCharsets.UTF_8));
  }
}
