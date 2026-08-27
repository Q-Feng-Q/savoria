package com.familykitchen.cart.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Versioned absolute current-member dish mutation.
 *
 * @param cartId active cart identifier, null only before the first mutation
 * @param cartVersion expected cart version
 * @param requestId durable client request identifier
 * @param dishId dish identifier
 * @param quantity absolute current-member quantity; zero removes only this member selection
 * @param itemRemark current-member item remark
 */
public record CartMutationRequest(Long cartId,@NotNull @Min(0) Long cartVersion,
    @NotBlank String requestId,@NotNull Long dishId,@Min(0) @Max(99) int quantity,String itemRemark) {}
