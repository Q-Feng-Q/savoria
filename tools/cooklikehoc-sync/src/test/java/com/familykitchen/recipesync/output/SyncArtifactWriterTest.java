package com.familykitchen.recipesync.output;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers byte-stable artifacts and release validation before writes. */
class SyncArtifactWriterTest {

  private final SyncArtifactWriter writer = new SyncArtifactWriter();
  private final SyncArtifacts artifacts = new SyncArtifacts(
      "{\"recipes\":[{\"sourceKey\":\"炒菜/示例.md\"}]}\n",
      "{\"unresolvedIssues\":0}\n",
      "INSERT INTO example VALUES (1);\n");

  @Test
  void identicalInputsProduceByteIdenticalDraftArtifacts(@TempDir Path tempDir) throws Exception {
    Path first = tempDir.resolve("first");
    Path second = tempDir.resolve("second");

    writer.writeDraft(first, artifacts);
    writer.writeDraft(second, artifacts);

    for (String name : List.of("normalized-recipes.json", "quality-report.json",
        "generated.sql", "artifact-sha256.json")) {
      assertArrayEquals(Files.readAllBytes(first.resolve(name)), Files.readAllBytes(second.resolve(name)));
    }
    String hashes = Files.readString(first.resolve("artifact-sha256.json"), StandardCharsets.UTF_8);
    assertFalse(hashes.matches("(?s).*20\\d{2}-\\d{2}-\\d{2}T.*"));
    assertFalse(hashes.contains(tempDir.toAbsolutePath().toString()));
  }

  @Test
  void unresolvedReleaseIssueCreatesNoTarget(@TempDir Path tempDir) {
    Path v4 = tempDir.resolve("V4.sql");
    Path manifest = tempDir.resolve("manifest.json");
    Path report = tempDir.resolve("report.json");

    assertThrows(IllegalStateException.class,
        () -> writer.writeRelease(v4, manifest, report, artifacts, List.of("MISSING_COMPONENT:x")));

    assertFalse(Files.exists(v4));
    assertFalse(Files.exists(manifest));
    assertFalse(Files.exists(report));
  }

  @Test
  void sqlRendererEscapesValuesAndKeepsEachRowOnOneLine() {
    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    values.put("id", 1L);
    values.put("name", "厨师's 菜");
    values.put("description", null);
    values.put("enabled", true);

    String sql = new SqlRenderer().insert("dish_templates", values);

    assertEquals("INSERT INTO dish_templates (id,name,description,enabled) VALUES "
        + "(1,'厨师''s 菜',NULL,1);\n", sql);
  }

  @Test
  void failedReleaseMoveRestoresEveryPreviouslyWrittenTarget(@TempDir Path tempDir)
      throws Exception {
    Path v4 = tempDir.resolve("V4.sql");
    Path manifest = tempDir.resolve("manifest.json");
    Path report = tempDir.resolve("report.json");
    String originalV4 = """
        -- BEGIN GENERATED COOKLIKEHOC DATA
        old row;
        -- END GENERATED COOKLIKEHOC DATA
        """;
    Files.writeString(v4, originalV4, StandardCharsets.UTF_8);
    Files.writeString(manifest, "old manifest", StandardCharsets.UTF_8);
    Files.writeString(report, "old report", StandardCharsets.UTF_8);
    int[] moves = {0};
    SyncArtifactWriter failingWriter = new SyncArtifactWriter((source, target) -> {
      if (++moves[0] == 2) {
        throw new IOException("simulated second move failure");
      }
      V4GeneratedSectionUpdater.atomicReplace(source, target);
    });

    assertThrows(IllegalArgumentException.class,
        () -> failingWriter.writeRelease(v4, manifest, report, artifacts, List.of()));

    assertEquals(originalV4, Files.readString(v4, StandardCharsets.UTF_8));
    assertEquals("old manifest", Files.readString(manifest, StandardCharsets.UTF_8));
    assertEquals("old report", Files.readString(report, StandardCharsets.UTF_8));
  }
}
