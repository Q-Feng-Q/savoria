package com.familykitchen.notebook.model;

import java.util.List;

/** Owner request to publish a replacement template version.
 * @param fields complete ordered field definition */
public record NotebookTemplateRequest(List<NotebookFieldInput> fields) {}
