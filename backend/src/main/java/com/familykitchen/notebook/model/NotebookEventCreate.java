package com.familykitchen.notebook.model;

import java.util.List;

/** Owner request to create an event and its first template together.
 * @param name event title
 * @param category optional category
 * @param description optional explanation
 * @param fields initial ordered template fields */
public record NotebookEventCreate(String name, String category, String description,
    List<NotebookFieldInput> fields) {}
