package com.familykitchen.cart.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Authoritative shared family-cart view.
 *
 * @param serverNow current Shanghai server time
 * @param serverDate current Shanghai server date
 * @param minimumExpectedMealTime minimum selectable time
 * @param timeStepMinutes selection grid minutes
 * @param bookingEnded whether no valid time remains today
 * @param cartId active cart identifier
 * @param familyId family identifier
 * @param version optimistic mutation version
 * @param expectedMealTime selected expected meal time
 * @param remark cart remark
 * @param totalQuantity aggregate quantity
 * @param totalAmount aggregate amount
 * @param items aggregate dish rows
 */
public record CartView(LocalDateTime serverNow,LocalDate serverDate,
    LocalDateTime minimumExpectedMealTime,int timeStepMinutes,boolean bookingEnded,
    Long cartId,Long familyId,long version,LocalDateTime expectedMealTime,String remark,
    int totalQuantity,BigDecimal totalAmount,List<CartItemView> items) {
  /**
   * Aggregate cart item.
   *
   * @param itemId aggregate item identifier
   * @param dishId dish identifier
   * @param dishName dish name
   * @param price unit price
   * @param quantity aggregate quantity
   * @param currentMemberQuantity current member quantity
   * @param currentMemberRemark current member remark
   * @param selections member attribution details
   * @param available whether the item can still be submitted
   * @param unavailableReason user-readable unavailable reason
   */
  public record CartItemView(Long itemId,Long dishId,String dishName,BigDecimal price,int quantity,
      int currentMemberQuantity,String currentMemberRemark,List<SelectionView> selections,
      boolean available,String unavailableReason) {}
  /**
   * Member attribution detail.
   *
   * @param memberId member identifier
   * @param memberName member display name
   * @param quantity selected quantity
   * @param itemRemark member remark
   */
  public record SelectionView(Long memberId,String memberName,int quantity,String itemRemark) {}
}
