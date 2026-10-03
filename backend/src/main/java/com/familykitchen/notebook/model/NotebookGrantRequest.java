package com.familykitchen.notebook.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Complete owner grant draft.
 * @param granteeUserId confirmed contact account
 * @param dataFrom inclusive local data date
 * @param dataTo inclusive local data date
 * @param dataTimeZone IANA zone for data dates
 * @param validFrom grant validity start instant
 * @param validTo grant validity end instant
 * @param canCreate independent create capability
 * @param canEdit independent edit capability
 * @param canExport independent export capability */
public record NotebookGrantRequest(long granteeUserId, LocalDate dataFrom, LocalDate dataTo,
    String dataTimeZone, OffsetDateTime validFrom, OffsetDateTime validTo,
    Boolean canCreate, Boolean canEdit, Boolean canExport) {}
