package com.familykitchen.recipesync.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Replaces only the unique generated data section in V4 using an atomic file move. */
public final class V4GeneratedSectionUpdater {

  public static final String BEGIN_MARKER = "-- BEGIN GENERATED COOKLIKEHOC DATA";
  public static final String END_MARKER = "-- END GENERATED COOKLIKEHOC DATA";

  /** Validates, renders and atomically writes an updated V4 file. */
  public void update(Path v4File, String generatedSql) {
    try {
      String current = Files.readString(v4File, StandardCharsets.UTF_8);
      String updated = render(current, generatedSql);
      Path parent = v4File.toAbsolutePath().getParent();
      Path temporary = Files.createTempFile(parent, v4File.getFileName().toString(), ".tmp");
      try {
        Files.writeString(temporary, updated, StandardCharsets.UTF_8);
        atomicReplace(temporary, v4File);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to update V4 generated section: " + v4File,
          exception);
    }
  }

  /** Produces replacement text after validating marker uniqueness and order. */
  public String render(String current, String generatedSql) {
    int begin = uniqueIndex(current, BEGIN_MARKER);
    int end = uniqueIndex(current, END_MARKER);
    if (begin > end) {
      throw new IllegalArgumentException("Generated V4 markers are reversed");
    }
    int contentStart = begin + BEGIN_MARKER.length();
    String normalizedGenerated = normalizeLf(generatedSql);
    if (!normalizedGenerated.endsWith("\n")) {
      normalizedGenerated += "\n";
    }
    return normalizeLf(current.substring(0, contentStart)) + "\n"
        + normalizedGenerated
        + normalizeLf(current.substring(end));
  }

  private int uniqueIndex(String value, String marker) {
    int first = value.indexOf(marker);
    if (first < 0 || value.indexOf(marker, first + marker.length()) >= 0) {
      throw new IllegalArgumentException("Expected exactly one V4 marker: " + marker);
    }
    return first;
  }

  private String normalizeLf(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }

  static void atomicReplace(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      throw new IOException("Atomic move is not supported for " + target, exception);
    }
  }
}
