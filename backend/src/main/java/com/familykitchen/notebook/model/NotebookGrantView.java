package com.familykitchen.notebook.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Persisted event-scoped share grant.
 * @param id grant ID
 * @param eventId event ID
 * @param ownerUserId owner account
 * @param granteeUserId confirmed contact account
 * @param dataFrom inclusive local data date
 * @param dataTo inclusive local data date
 * @param dataTimeZone IANA zone for data dates
 * @param validFrom validity start
 * @param validTo validity end
 * @param canCreate independent create permission
 * @param canEdit independent edit permission
 * @param canExport independent export permission
 * @param status grant status */
public record NotebookGrantView(long id, long eventId, long ownerUserId, long granteeUserId,
    LocalDate dataFrom, LocalDate dataTo, String dataTimeZone,
    OffsetDateTime validFrom, OffsetDateTime validTo,
    boolean canCreate, boolean canEdit, boolean canExport, String status) {}
