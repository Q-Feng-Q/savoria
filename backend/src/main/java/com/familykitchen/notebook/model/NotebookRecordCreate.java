package com.familykitchen.notebook.model;

import java.time.OffsetDateTime;
import java.util.Map;

/** Draft record bound to the event's current template.
 * @param occurredFrom inclusive occurrence start
 * @param occurredTo inclusive occurrence end
 * @param title record title
 * @param note optional note
 * @param values field-keyed values */
public record NotebookRecordCreate(OffsetDateTime occurredFrom, OffsetDateTime occurredTo,
    String title, String note, Map<String, Object> values) {}
