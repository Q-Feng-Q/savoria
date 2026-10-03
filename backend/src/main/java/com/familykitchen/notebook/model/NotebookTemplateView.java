package com.familykitchen.notebook.model;

import java.util.List;

/** Published field definitions for one immutable event version.
 * @param eventId containing event
 * @param version monotonically increasing version number
 * @param fields ordered published fields */
public record NotebookTemplateView(long eventId, int version, List<NotebookField> fields) {}
