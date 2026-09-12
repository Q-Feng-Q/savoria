package com.familykitchen.recipesync.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes deterministic draft artifacts and guarded release targets. */
public final class SyncArtifactWriter {

  private static final String MANIFEST_NAME = "normalized-recipes.json";
  private static final String REPORT_NAME = "quality-report.json";
  private static final String SQL_NAME = "generated.sql";
  private final AtomicMover atomicMover;

  public SyncArtifactWriter() {
    this(V4GeneratedSectionUpdater::atomicReplace);
  }

  SyncArtifactWriter(AtomicMover atomicMover) {
    this.atomicMover = atomicMover;
  }

  /** Writes reviewable draft artifacts and their content-only hashes. */
  public void writeDraft(Path outputDirectory, SyncArtifacts artifacts) {
    try {
      Files.createDirectories(outputDirectory);
      Map<String, byte[]> contents = artifactBytes(artifacts);
      for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
        Files.write(outputDirectory.resolve(entry.getKey()), entry.getValue());
      }
      Files.writeString(outputDirectory.resolve("artifact-sha256.json"), hashManifest(contents),
          StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to write draft artifacts: " + outputDirectory,
          exception);
    }
  }

  /** Writes all release targets only after every explicit issue has been resolved. */
  public void writeRelease(Path v4File, Path manifestFile, Path qualityReportFile,
      SyncArtifacts artifacts, List<String> unresolvedIssues) {
    if (unresolvedIssues != null && !unresolvedIssues.isEmpty()) {
      throw new IllegalStateException("RELEASE_BLOCKED:" + String.join(",", unresolvedIssues));
    }
    if (!Files.exists(v4File)) {
      throw new IllegalArgumentException("V4 target does not exist: " + v4File);
    }
    try {
      String currentV4 = Files.readString(v4File, StandardCharsets.UTF_8);
      String updatedV4 = new V4GeneratedSectionUpdater().render(currentV4, artifacts.generatedSql());
      Files.createDirectories(manifestFile.toAbsolutePath().getParent());
      Files.createDirectories(qualityReportFile.toAbsolutePath().getParent());
      List<TargetState> targets = List.of(
          targetState(v4File, updatedV4.getBytes(StandardCharsets.UTF_8)),
          targetState(manifestFile, artifacts.normalizedRecipesJson().getBytes(StandardCharsets.UTF_8)),
          targetState(qualityReportFile, artifacts.qualityReportJson().getBytes(StandardCharsets.UTF_8)));
      int committed = 0;
      try {
        for (TargetState target : targets) {
          atomicMover.move(target.stagedFile(), target.targetFile());
          committed++;
        }
      } catch (IOException releaseFailure) {
        rollback(targets, committed, releaseFailure);
        throw releaseFailure;
      } finally {
        for (TargetState target : targets) {
          Files.deleteIfExists(target.stagedFile());
        }
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to write release artifacts", exception);
    }
  }

  private Path stage(Path target, byte[] content) throws IOException {
    Path temporary = Files.createTempFile(target.toAbsolutePath().getParent(),
        target.getFileName().toString(), ".tmp");
    Files.write(temporary, content);
    return temporary;
  }

  private TargetState targetState(Path target, byte[] newContent) throws IOException {
    boolean existed = Files.exists(target);
    byte[] original = existed ? Files.readAllBytes(target) : null;
    return new TargetState(target, stage(target, newContent), existed, original);
  }

  private void rollback(List<TargetState> targets, int committed, IOException releaseFailure) {
    for (int index = committed - 1; index >= 0; index--) {
      TargetState target = targets.get(index);
      try {
        if (target.existed()) {
          Path restore = stage(target.targetFile(), target.originalContent());
          try {
            V4GeneratedSectionUpdater.atomicReplace(restore, target.targetFile());
          } finally {
            Files.deleteIfExists(restore);
          }
        } else {
          Files.deleteIfExists(target.targetFile());
        }
      } catch (IOException rollbackFailure) {
        releaseFailure.addSuppressed(rollbackFailure);
      }
    }
  }

  private Map<String, byte[]> artifactBytes(SyncArtifacts artifacts) {
    Map<String, byte[]> result = new LinkedHashMap<>();
    result.put(SQL_NAME, artifacts.generatedSql().getBytes(StandardCharsets.UTF_8));
    result.put(MANIFEST_NAME, artifacts.normalizedRecipesJson().getBytes(StandardCharsets.UTF_8));
    result.put(REPORT_NAME, artifacts.qualityReportJson().getBytes(StandardCharsets.UTF_8));
    return result;
  }

  private String hashManifest(Map<String, byte[]> contents) {
    StringBuilder json = new StringBuilder("{\n");
    int index = 0;
    for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
      if (index++ > 0) {
        json.append(",\n");
      }
      json.append("  \"").append(entry.getKey()).append("\": \"")
          .append(sha256(entry.getValue())).append('"');
    }
    return json.append("\n}\n").toString();
  }

  private String sha256(byte[] value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte current : digest) {
        hex.append(String.format("%02x", current));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  @FunctionalInterface
  interface AtomicMover {
    void move(Path source, Path target) throws IOException;
  }

  private record TargetState(
      Path targetFile,
      Path stagedFile,
      boolean existed,
      byte[] originalContent) {
  }
}
