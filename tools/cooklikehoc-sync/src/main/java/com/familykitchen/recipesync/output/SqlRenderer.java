package com.familykitchen.recipesync.output;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/** Renders deterministic one-row-per-line MySQL INSERT statements. */
public final class SqlRenderer {

  /** Renders one ordered INSERT using the map's iteration order. */
  public String insert(String table, Map<String, Object> values) {
    requireIdentifier(table);
    if (values.isEmpty()) {
      throw new IllegalArgumentException("SQL INSERT requires at least one column");
    }
    values.keySet().forEach(this::requireIdentifier);
    String columns = String.join(",", values.keySet());
    String renderedValues = values.values().stream()
        .map(this::literal)
        .collect(Collectors.joining(","));
    return "INSERT INTO " + table + " (" + columns + ") VALUES (" + renderedValues + ");\n";
  }

  private String literal(Object value) {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue ? "1" : "0";
    }
    if (value instanceof Number number) {
      return number instanceof BigDecimal decimal ? decimal.toPlainString() : number.toString();
    }
    String text = value.toString()
        .replace("\r\n", " ")
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace("'", "''");
    return "'" + text + "'";
  }

  private void requireIdentifier(String value) {
    if (value == null || !value.matches("[a-z][a-z0-9_]*")) {
      throw new IllegalArgumentException("Invalid SQL identifier: " + value);
    }
  }
}
