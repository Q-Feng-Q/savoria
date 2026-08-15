package com.familykitchen.purchase.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 承载Temp采购项目相关的请求参数。
 *
 * @param date date
 * @param mealSlotId mealSlot标识
 * @param ingredientName 食材名称
 * @param quantity quantity
 * @param unit unit
 * @param remark 备注
 */
public record TempPurchaseItemRequest(
    @NotNull LocalDate date,
    Long mealSlotId,
    @NotBlank String ingredientName,
    @NotNull @DecimalMin("0.00") BigDecimal quantity,
    @NotBlank String unit,
    String remark
) {
}

