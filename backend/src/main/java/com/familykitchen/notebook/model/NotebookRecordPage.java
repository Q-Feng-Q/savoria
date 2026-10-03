package com.familykitchen.notebook.model;

import java.util.List;

/** One bounded page of records.
 * @param items records in chronological order
 * @param page zero-based page number
 * @param size maximum page size
 * @param hasMore whether a subsequent page exists */
public record NotebookRecordPage(List<NotebookRecordView> items, int page, int size, boolean hasMore) {}
