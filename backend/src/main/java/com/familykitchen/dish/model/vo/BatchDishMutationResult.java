package com.familykitchen.dish.model.vo;

/** Truthful counts for one batch delete or restore request.
 * @param requestedCount raw request item count
 * @param uniqueCount deduplicated item count
 * @param changedCount items whose state changed
 * @param unchangedCount idempotent items whose state was already correct
 */
public record BatchDishMutationResult(
    int requestedCount,
    int uniqueCount,
    int changedCount,
    int unchangedCount
) {
}
