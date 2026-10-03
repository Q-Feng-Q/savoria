package com.familykitchen.notebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.model.NotebookFieldInput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Validates a complete template and assigns keys without changing historical definitions. */
@Component
public class NotebookTemplateValidator {
  private static final int MAX_STORED_JSON_BYTES = 60_000;
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Set<String> TYPES = Set.of("TEXT", "LONG_TEXT", "NUMBER", "DATE", "TIME",
      "DATETIME", "SINGLE_SELECT", "MULTI_SELECT", "BOOLEAN", "RATING", "IMAGE");

  /** Produces a new immutable version from an owner draft.
   * @param inputs complete ordered draft fields
   * @param previous fields in the current published version
   * @return validated fields with stable or newly assigned keys */
  public List<NotebookField> publish(List<NotebookFieldInput> inputs, List<NotebookField> previous) {
    if (inputs == null || inputs.size() > 50) throw bad("Template requires at most 50 fields");
    Map<String, NotebookField> existing = new HashMap<>();
    if (previous != null) for (NotebookField field : previous) existing.put(field.key(), field);
    Set<String> submittedKeys = new HashSet<>();
    List<NotebookField> result = new ArrayList<>();
    for (NotebookFieldInput input : inputs) {
      if (input == null || input.type() == null || !TYPES.contains(input.type())
          || input.label() == null || input.label().isBlank() || input.label().strip().length() > 120
          || input.required() == null) throw bad("Invalid template field");
      if (input.key() != null && (!submittedKeys.add(input.key()) || !existing.containsKey(input.key()))) {
        throw bad("Unknown or duplicate template field key");
      }
      boolean select = input.type().equals("SINGLE_SELECT") || input.type().equals("MULTI_SELECT");
      List<String> options = input.options() == null ? List.of() : input.options();
      if (select && (options.isEmpty() || options.size() > 30)) throw bad("Select fields require options");
      if (!select && !options.isEmpty()) throw bad("Options are only valid for select fields");
      List<String> normalized = new ArrayList<>();
      for (String option : options) {
        if (option == null || option.isBlank() || option.strip().length() > 120
            || normalized.contains(option.strip())) throw bad("Invalid or duplicate option");
        normalized.add(option.strip());
      }
      String unit = input.unit() == null || input.unit().isBlank() ? null : input.unit().strip();
      if (unit != null && (!input.type().equals("NUMBER") || unit.length() > 30)) {
        throw bad("Unit is only supported by numeric fields");
      }
      NotebookField old = existing.get(input.key());
      String key = old != null && old.type().equals(input.type()) ? old.key()
          : "field_" + UUID.randomUUID().toString().replace("-", "");
      result.add(new NotebookField(key, input.type(), input.label().strip(), input.required(),
          List.copyOf(normalized), unit));
    }
    List<NotebookField> published = List.copyOf(result);
    try {
      if (JSON.writeValueAsBytes(published).length > MAX_STORED_JSON_BYTES) {
        throw bad("Template is too large");
      }
    } catch (JsonProcessingException failure) {
      throw bad("Invalid template fields");
    }
    return published;
  }

  private static BusinessException bad(String message) {
    return new BusinessException(ErrorCode.BAD_REQUEST, message);
  }
}
