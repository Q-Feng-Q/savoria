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
 * @param status grant status
 * @param eventName event label on recipient discovery, null for owner grant management */
public record NotebookGrantView(long id, long eventId, long ownerUserId, long granteeUserId,
    LocalDate dataFrom, LocalDate dataTo, String dataTimeZone,
    OffsetDateTime validFrom, OffsetDateTime validTo,
    boolean canCreate, boolean canEdit, boolean canExport, String status, String eventName) {
  /** Preserves grant-only construction outside recipient discovery.
   * @param id grant ID
   * @param eventId event ID
   * @param ownerUserId owner account
   * @param granteeUserId recipient account
   * @param dataFrom inclusive local data date
   * @param dataTo inclusive local data date
   * @param dataTimeZone IANA zone for data dates
   * @param validFrom validity start
   * @param validTo validity end
   * @param canCreate create permission
   * @param canEdit edit permission
   * @param canExport export permission
   * @param status grant status */
  public NotebookGrantView(long id, long eventId, long ownerUserId, long granteeUserId,
      LocalDate dataFrom, LocalDate dataTo, String dataTimeZone,
      OffsetDateTime validFrom, OffsetDateTime validTo,
      boolean canCreate, boolean canEdit, boolean canExport, String status) {
    this(id, eventId, ownerUserId, granteeUserId, dataFrom, dataTo, dataTimeZone,
        validFrom, validTo, canCreate, canEdit, canExport, status, null);
  }

  /** Adds the label visible to the confirmed recipient without exposing the description.
   * @param name event label
   * @return grant view with its event label */
  public NotebookGrantView withEventName(String name) {
    return new NotebookGrantView(id, eventId, ownerUserId, granteeUserId, dataFrom, dataTo,
        dataTimeZone, validFrom, validTo, canCreate, canEdit, canExport, status, name);
  }
}
