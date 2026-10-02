package com.familykitchen.notebook.model;

/** Counts shown before the owner confirms permanent event removal.
 * @param recordCount affected records
 * @param templateVersionCount affected template versions
 * @param grantCount affected sharing grants */
public record NotebookDeleteImpact(long recordCount, long templateVersionCount, long grantCount) {}
