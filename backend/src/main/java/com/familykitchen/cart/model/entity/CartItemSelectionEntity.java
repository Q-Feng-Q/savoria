package com.familykitchen.cart.model.entity;

/** One family member's absolute quantity and remark for an aggregate cart item. */
public class CartItemSelectionEntity {
  /** Selection identifier. */ public Long id;
  /** Aggregate item identifier. */ public Long cartItemId;
  /** Selecting user identifier. */ public Long userId;
  /** Selecting member display name. */ public String memberName;
  /** Absolute member quantity. */ public Integer quantity;
  /** Member-specific remark. */ public String itemRemark;
}
