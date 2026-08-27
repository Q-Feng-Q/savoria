package com.familykitchen.cart.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Versioned expected-meal-time mutation.
 *
 * @param cartId active cart identifier
 * @param cartVersion expected cart version
 * @param requestId durable client request identifier
 * @param expectedMealTime selected today-only business time
 */
public record ExpectedMealTimeRequest(@NotNull Long cartId,@NotNull @Min(0) Long cartVersion,
    @NotBlank String requestId,@NotNull LocalDateTime expectedMealTime) {}
