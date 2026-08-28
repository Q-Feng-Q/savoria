package com.familykitchen.order.model.bo;

import java.time.LocalDateTime;
import java.util.List;

/** Locked shared-cart snapshot used during submission.
 * @param cartId cart identifier
 * @param merchantId merchant identifier
 * @param familyId family identifier
 * @param version locked cart version
 * @param expectedMealTime expected meal time
 * @param remark cart remark
 * @param items aggregate items
 */
public record OrderCartSnapshot(Long cartId,Long merchantId,Long familyId,Long version,
    LocalDateTime expectedMealTime,String remark,List<CheckoutItem> items) {}
