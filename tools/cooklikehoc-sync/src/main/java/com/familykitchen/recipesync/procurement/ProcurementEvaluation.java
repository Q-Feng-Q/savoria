package com.familykitchen.recipesync.procurement;

import java.util.List;

/** Deterministic procurement readiness result with explicit blocking reasons. */
public record ProcurementEvaluation(
    boolean ready,
    List<String> blockingReasons,
    List<AggregatedProcurementItem> aggregatedItems) {
}
