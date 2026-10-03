package com.familykitchen.notebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.model.NotebookField;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Checks record values against the exact immutable template version. */
@Component
public class NotebookRecordValidator {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int MAX_JSON_BYTES = 60_000;

  /** Validates a complete field-keyed value object.
   * @param fields fields of the record's stored template version
   * @param values complete submitted values */
  public void validate(List<NotebookField> fields, Map<String, Object> values) {
    if (fields == null || values == null) throw bad("Record values are required");
    Set<String> allowed = new HashSet<>();
    for (NotebookField field : fields) {
      allowed.add(field.key());
      Object value = values.get(field.key());
      if (value == null || value instanceof String text && text.isBlank()
          || value instanceof List<?> list && list.isEmpty()) {
        if (field.required()) throw bad("Required notebook field is missing: " + field.key());
        continue;
      }
      if (!valid(field, value)) throw bad("Invalid notebook field value: " + field.key());
    }
    if (!allowed.containsAll(values.keySet())) throw bad("Unknown notebook field key");
    try {
      if (JSON.writeValueAsBytes(values).length > MAX_JSON_BYTES) throw bad("Record values are too large");
    } catch (JsonProcessingException failure) {
      throw bad("Invalid record values");
    }
  }

  private boolean valid(NotebookField field, Object value) {
    try {
      return switch (field.type()) {
        case "TEXT" -> value instanceof String text && text.length() <= 2000;
        case "LONG_TEXT" -> value instanceof String text && text.length() <= 20000;
        case "NUMBER" -> value instanceof Number number
            && Double.isFinite(number.doubleValue());
        case "DATE" -> value instanceof String text && LocalDate.parse(text) != null;
        case "TIME" -> value instanceof String text && LocalTime.parse(text) != null;
        case "DATETIME" -> value instanceof String text && OffsetDateTime.parse(text) != null;
        case "SINGLE_SELECT" -> value instanceof String text && field.options().contains(text);
        case "MULTI_SELECT" -> value instanceof List<?> list && list.size() <= field.options().size()
            && new HashSet<>(list).size() == list.size()
            && list.stream().allMatch(item -> item instanceof String && field.options().contains(item));
        case "BOOLEAN" -> value instanceof Boolean;
        case "RATING" -> value instanceof Number number && number.intValue() == number.doubleValue()
            && number.intValue() >= 1 && number.intValue() <= 5;
        case "IMAGE" -> value instanceof List<?> list && list.size() <= 10
            && list.stream().allMatch(item -> item instanceof String key
                && key.matches("[a-fA-F0-9-]{32,36}\\.(png|jpg|webp)"));
        default -> false;
      };
    } catch (DateTimeParseException failure) {
      return false;
    }
  }

  private static BusinessException bad(String message) {
    return new BusinessException(ErrorCode.BAD_REQUEST, message);
  }
}
