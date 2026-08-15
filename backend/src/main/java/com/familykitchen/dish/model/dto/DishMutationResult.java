package com.familykitchen.dish.model.dto;

/**
 * Reports the truthful persistence outcome of a dish mutation.
 * @param outcome applied immediately or submitted for review
 */
public record DishMutationResult(Outcome outcome) {
  /** Supported dish mutation outcomes. */
  public enum Outcome {
    /** Mutation was applied to the active dish immediately. */
    APPLIED,
    /** Mutation was stored as a review submission. */
    PENDING_REVIEW
  }
}
