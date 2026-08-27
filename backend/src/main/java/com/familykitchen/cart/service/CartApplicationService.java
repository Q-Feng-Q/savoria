package com.familykitchen.cart.service;

import com.familykitchen.cart.model.dto.CartMutationRequest;
import com.familykitchen.cart.model.dto.CartRemarkRequest;
import com.familykitchen.cart.model.dto.ExpectedMealTimeRequest;
import com.familykitchen.cart.model.vo.CartView;
import com.familykitchen.common.security.CurrentUserContext;

/** Shared family-cart application service. */
public interface CartApplicationService {

  /**
   * Returns the family's current active cart and authoritative booking metadata.
   *
   * @param user current user
   * @return authoritative cart view
   */
  CartView cart(CurrentUserContext user);

  /**
   * Sets the current member's absolute quantity for one dish.
   *
   * @param user current user
   * @param request versioned mutation
   * @return authoritative cart view
   */
  CartView mutateItem(CurrentUserContext user, CartMutationRequest request);

  /**
   * Updates the expected meal time.
   *
   * @param user current user
   * @param request versioned mutation
   * @return authoritative cart view
   */
  CartView updateExpectedMealTime(CurrentUserContext user, ExpectedMealTimeRequest request);

  /**
   * Updates the shared cart remark.
   *
   * @param user current user
   * @param request versioned mutation
   * @return authoritative cart view
   */
  CartView updateRemark(CurrentUserContext user, CartRemarkRequest request);
}
