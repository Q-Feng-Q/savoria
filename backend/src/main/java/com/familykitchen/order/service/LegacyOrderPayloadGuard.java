package com.familykitchen.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.util.List;

/** Rejects retired order-write fields before binding a new submission request. */
public final class LegacyOrderPayloadGuard {
  private static final List<String> RETIRED = List.of(
      "mealSlotId", "serviceDate", "date", "payerMemberId", "deliveryFeePayerUserId");

  private LegacyOrderPayloadGuard() {}

  /** Rejects payloads produced by a pre-shared-cart client.
   * @param body raw JSON request body
   */
  public static void requireCompatible(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "订单请求内容不能为空");
    }
    for (String field : RETIRED) {
      if (body.has(field)) {
        throw new BusinessException(ErrorCode.CLIENT_UPGRADE_REQUIRED,
            "客户端版本过旧，请升级后重新提交订单");
      }
    }
  }
}
