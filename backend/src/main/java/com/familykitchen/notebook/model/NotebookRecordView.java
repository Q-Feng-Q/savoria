package com.familykitchen.notebook.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Owner-visible record and its immutable template definition.
 * @param id record ID
 * @param eventId parent event
 * @param ownerUserId owner account
 * @param createdByUserId author account
 * @param updatedByUserId last editor account
 * @param occurredFrom inclusive start
 * @param occurredTo inclusive end
 * @param title title
 * @param note optional note
 * @param templateVersion bound template version
 * @param values field values
 * @param fields bound template fields
 * @param lockVersion optimistic revision */
public record NotebookRecordView(long id, long eventId, long ownerUserId, long createdByUserId,
    long updatedByUserId, OffsetDateTime occurredFrom, OffsetDateTime occurredTo,
    String title, String note, int templateVersion, Map<String, Object> values,
    List<NotebookField> fields, int lockVersion) {}
