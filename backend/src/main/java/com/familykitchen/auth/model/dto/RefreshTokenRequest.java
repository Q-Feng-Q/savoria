package com.familykitchen.auth.model.dto;

import jakarta.validation.constraints.NotBlank;

/** Rotating mini-program refresh credential.
 * @param refreshToken presented credential */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
