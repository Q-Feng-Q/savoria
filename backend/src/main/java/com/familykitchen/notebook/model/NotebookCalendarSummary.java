package com.familykitchen.notebook.model;

import java.time.LocalDate;

/** Date-level projection without record field values.
 * @param date local calendar day
 * @param recordCount intersecting record count */
public record NotebookCalendarSummary(LocalDate date, long recordCount) {}
