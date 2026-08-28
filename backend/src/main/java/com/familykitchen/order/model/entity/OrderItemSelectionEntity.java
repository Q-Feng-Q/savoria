package com.familykitchen.order.model.entity;

/** Immutable member attribution snapshot below an aggregate order item. */
public class OrderItemSelectionEntity {
  /** Selection ID. */ public Long id;
  /** Order item ID. */ public Long orderItemId;
  /** Selecting user ID. */ public Long userId;
  /** Positive selected quantity. */ public Integer quantity;
  /** Member display name snapshot. */ public String memberNameSnapshot;
  /** Member-specific remark snapshot. */ public String itemRemark;
}
