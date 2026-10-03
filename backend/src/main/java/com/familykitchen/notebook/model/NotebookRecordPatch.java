package com.familykitchen.notebook.model;

import java.time.OffsetDateTime;
import java.util.Map;

/** Optimistic record edit against its original template version.
 * @param expectedVersion last observed lock version
 * @param title optional replacement title
 * @param note optional replacement note
 * @param occurredFrom optional replacement start
 * @param occurredTo optional replacement end
 * @param values optional complete field-value replacement */
public record NotebookRecordPatch(Integer expectedVersion, String title, String note,
    OffsetDateTime occurredFrom, OffsetDateTime occurredTo, Map<String, Object> values) {}
