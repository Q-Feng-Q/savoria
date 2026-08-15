package com.familykitchen.cart.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 承载购物车备注相关的请求参数。
 *
 * @param mealSlotId mealSlot标识
 * @param date date
 * @param remark 备注
 */
public record CartRemarkRequest(
    @NotNull Long mealSlotId,
    @NotNull LocalDate date,
    String remark
) {
}

