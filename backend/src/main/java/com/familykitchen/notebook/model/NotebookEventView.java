package com.familykitchen.notebook.model;

/** Account-owned event summary.
 * @param id event ID
 * @param ownerUserId owning account ID
 * @param name event name
 * @param category optional category
 * @param description optional description
 * @param sortOrder display order
 * @param starred focus flag
 * @param archived archive flag
 * @param currentTemplateVersion latest published template version */
public record NotebookEventView(long id, long ownerUserId, String name, String category,
    String description, int sortOrder, boolean starred, boolean archived,
    int currentTemplateVersion) {}
