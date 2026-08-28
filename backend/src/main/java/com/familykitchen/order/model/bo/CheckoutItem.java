package com.familykitchen.order.model.bo;

import java.math.BigDecimal;
import java.util.List;

/** Aggregate dish row and its per-member selection snapshot.
 * @param dishId dish identifier
 * @param dishName dish name snapshot
 * @param price family price snapshot
 * @param quantity aggregate quantity
 * @param itemRemark aggregate remark
 * @param ingredients ingredient snapshot
 * @param selections per-member selections
 */
public record CheckoutItem(Long dishId,String dishName,BigDecimal price,int quantity,
    String itemRemark,List<CheckoutIngredient> ingredients,List<MemberSelection> selections) {
  /** One selecting member.
   * @param userId member identifier
   * @param memberName immutable display name
   * @param quantity positive selected quantity
   * @param itemRemark member-specific remark
   */
  public record MemberSelection(Long userId,String memberName,int quantity,String itemRemark) {}
}
