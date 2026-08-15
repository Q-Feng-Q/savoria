package com.familykitchen.cart.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 承载购物车项目相关的请求参数。
 *
 * @param mealSlotId mealSlot标识
 * @param date date
 * @param dishId 菜品标识
 * @param quantity quantity
 * @param itemRemark 项目备注
 */
public record CartItemRequest(
    @NotNull Long mealSlotId,
    @NotNull LocalDate date,
    @NotNull Long dishId,
    @Min(1) int quantity,
    String itemRemark
) {
}

