package com.familykitchen.notebook.model;

/** Partial owner update to event display and organization.
 * @param name new name, or null to retain
 * @param category new category, or null to retain
 * @param description new description, or null to retain
 * @param starred focus flag, or null to retain
 * @param archived archive flag, or null to retain
 * @param sortOrder new display order, or null to retain */
public record NotebookEventPatch(String name, String category, String description,
    Boolean starred, Boolean archived, Integer sortOrder) {}
