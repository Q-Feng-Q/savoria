package com.familykitchen.order.model.dto;

import com.familykitchen.order.model.enums.DeliveryMode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 承载Submit订单相关的请求参数。
 *
 * @param date date
 * @param mealSlotId mealSlot标识
 * @param deliveryMode 配送Mode
 * @param addressId 地址标识
 * @param remark 备注
 */
public record SubmitOrderRequest(
    @NotNull LocalDate date,
    @NotNull Long mealSlotId,
    @NotNull DeliveryMode deliveryMode,
    Long addressId,
    String remark
) {
}

