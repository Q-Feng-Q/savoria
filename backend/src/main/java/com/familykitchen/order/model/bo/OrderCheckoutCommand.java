package com.familykitchen.order.model.bo;

import com.familykitchen.order.model.enums.DeliveryMode;
import java.time.LocalDateTime;
import java.util.List;

/** Immutable shared-cart checkout command.
 * @param sourceCartId source cart identifier
 * @param merchantId merchant identifier
 * @param familyId family identifier
 * @param submitterMemberId submitting member identifier
 * @param expectedMealTime expected meal time
 * @param deliveryMode requested delivery mode
 * @param remark order remark
 * @param deliverySnapshot delivery address snapshot
 * @param familyDeliveryPolicy family delivery policy
 * @param items aggregate items
 */
public record OrderCheckoutCommand(Long sourceCartId,Long merchantId,Long familyId,
    Long submitterMemberId,LocalDateTime expectedMealTime,DeliveryMode deliveryMode,
    String remark,DeliverySnapshot deliverySnapshot,FamilyDeliveryPolicy familyDeliveryPolicy,
    List<CheckoutItem> items) {}
