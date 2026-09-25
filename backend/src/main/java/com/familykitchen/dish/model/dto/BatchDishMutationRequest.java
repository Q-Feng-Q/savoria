package com.familykitchen.dish.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Requests one atomic merchant-scoped batch dish mutation.
 * @param dishIds one to one hundred positive dish identifiers
 */
public record BatchDishMutationRequest(
    @NotNull @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> dishIds
) {
}
