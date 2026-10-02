package com.familykitchen.notebook.model;

import java.util.List;

/** Published immutable template field.
 * @param key server assigned stable field key
 * @param type field type
 * @param label display label
 * @param required whether a record value is required
 * @param options ordered choices for select fields
 * @param unit optional numeric unit */
public record NotebookField(String key, String type, String label, boolean required,
    List<String> options, String unit) {}
