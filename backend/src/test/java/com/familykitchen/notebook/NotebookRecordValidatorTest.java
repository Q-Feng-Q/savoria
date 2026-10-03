package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.service.NotebookRecordValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Exercises every supported notebook record value type. */
class NotebookRecordValidatorTest {
  private final NotebookRecordValidator validator = new NotebookRecordValidator();

  @Test void acceptsAllElevenFieldTypes() {
    var fields = List.of(
        field("text", "TEXT"), field("long", "LONG_TEXT"), field("number", "NUMBER"),
        field("date", "DATE"), field("time", "TIME"), field("datetime", "DATETIME"),
        select("single", "SINGLE_SELECT"), select("multi", "MULTI_SELECT"),
        field("boolean", "BOOLEAN"), field("rating", "RATING"), field("image", "IMAGE"));
    validator.validate(fields, Map.ofEntries(Map.entry("text", "hello"), Map.entry("long", "notes"),
        Map.entry("number", 2.5), Map.entry("date", "2026-01-02"), Map.entry("time", "10:30:00"),
        Map.entry("datetime", "2026-01-02T10:30:00+08:00"), Map.entry("single", "one"),
        Map.entry("multi", List.of("one", "two")), Map.entry("boolean", true),
        Map.entry("rating", 5),
        Map.entry("image", List.of("12e45678-e89b-12d3-a456-426614174000.png"))));
  }

  @Test void rejectsMissingRequiredAndWrongTypes() {
    assertThatThrownBy(() -> validator.validate(List.of(field("text", "TEXT")), Map.of()))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> validator.validate(List.of(field("rating", "RATING")), Map.of("rating", 6)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> validator.validate(List.of(select("single", "SINGLE_SELECT")),
        Map.of("single", "unknown"))).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> validator.validate(List.of(field("text", "TEXT")),
        Map.of("text", 2))).isInstanceOf(BusinessException.class);
  }

  @Test void rejectsInvalidValueForEachSupportedType() {
    Map<String, Object> invalid = Map.ofEntries(
        Map.entry("TEXT", 2), Map.entry("LONG_TEXT", 2), Map.entry("NUMBER", "NaN"),
        Map.entry("DATE", "2026-02-31"), Map.entry("TIME", "25:00"),
        Map.entry("DATETIME", "2026-01-01T10:00:00"), Map.entry("SINGLE_SELECT", "other"),
        Map.entry("MULTI_SELECT", List.of("one", "one")), Map.entry("BOOLEAN", "true"),
        Map.entry("RATING", 0), Map.entry("IMAGE", List.of("public/photo.png")));
    invalid.forEach((type, value) -> {
      NotebookField field = type.contains("SELECT") ? select("value", type) : field("value", type);
      assertThatThrownBy(() -> validator.validate(List.of(field), Map.of("value", value)))
          .as(type).isInstanceOf(BusinessException.class);
    });
  }

  private static NotebookField field(String key, String type) {
    return new NotebookField(key, type, key, true, List.of(), null);
  }

  private static NotebookField select(String key, String type) {
    return new NotebookField(key, type, key, true, List.of("one", "two"), null);
  }
}
