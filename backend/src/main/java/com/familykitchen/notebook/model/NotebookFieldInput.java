package com.familykitchen.notebook.model;

import java.util.List;

/** Draft template field supplied by the owner.
 * @param key existing server key when editing, otherwise null
 * @param type supported field type
 * @param label display label
 * @param required whether a record value is required
 * @param options ordered choices for select fields
 * @param unit optional numeric unit */
public record NotebookFieldInput(String key, String type, String label, Boolean required,
    List<String> options, String unit) {}
