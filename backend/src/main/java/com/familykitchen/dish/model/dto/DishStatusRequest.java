package com.familykitchen.dish.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Requests a canonical availability status transition for one dish.
 * @param status canonical status, either ACTIVE or INACTIVE
 */
public record DishStatusRequest(
    @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status
) {}
