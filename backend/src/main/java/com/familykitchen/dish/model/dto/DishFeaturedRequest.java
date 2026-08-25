package com.familykitchen.dish.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request for enabling or disabling a merchant-wide featured dish.
 * @param featured required target state
 */
public record DishFeaturedRequest(@NotNull Boolean featured) {
}
