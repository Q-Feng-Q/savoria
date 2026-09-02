package com.familykitchen.recipesync.procurement;

import java.util.List;

/** Template node participating in procurement graph evaluation. */
public record ProcurementTemplate(String key, List<ProcurementItem> items) {
}
