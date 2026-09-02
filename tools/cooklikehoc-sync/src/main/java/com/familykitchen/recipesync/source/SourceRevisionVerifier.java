package com.familykitchen.recipesync.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Reads the checked-out Git revision and rejects an unexpected source tree. */
public final class SourceRevisionVerifier {

  /** Verifies that the source directory is exactly at the configured commit. */
  public void verify(Path sourceDir, String expectedRevision) {
    if (!Files.isDirectory(sourceDir)) {
      throw new IllegalArgumentException("Source directory does not exist: " + sourceDir);
    }
    Process process = startGit(sourceDir);
    try {
      if (!process.waitFor(15, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IllegalStateException("SOURCE_REVISION_CHECK_TIMEOUT");
      }
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (process.exitValue() != 0) {
        throw new IllegalStateException("SOURCE_REVISION_CHECK_FAILED");
      }
      if (!expectedRevision.equals(output)) {
        throw new IllegalStateException("SOURCE_REVISION_MISMATCH");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("SOURCE_REVISION_CHECK_INTERRUPTED", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("SOURCE_REVISION_OUTPUT_FAILED", exception);
    }
  }

  private Process startGit(Path sourceDir) {
    try {
      return new ProcessBuilder("git", "-C", sourceDir.toString(), "rev-parse", "HEAD")
          .redirectErrorStream(true)
          .start();
    } catch (IOException exception) {
      throw new IllegalStateException("SOURCE_REVISION_CHECK_FAILED", exception);
    }
  }
}
