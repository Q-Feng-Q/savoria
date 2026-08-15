package com.familykitchen.family.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 封装返回给调用方的家庭首页数据。
 *
 * @param family 家庭
 * @param member 成员
 * @param serviceDate serviceDate
 * @param featuredDish featured菜品
 * @param dashboardCards dashboardCards
 * @param mealSlots mealSlots
 * @param recentOrders recentOrders
 */
public record FamilyHomeResponse(
    FamilySummary family,
    MemberSummary member,
    LocalDate serviceDate,
    FeaturedDish featuredDish,
    List<DashboardCard> dashboardCards,
    List<MealSlotView> mealSlots,
    List<OrderSummary> recentOrders
) {

  /**
   * 封装返回给调用方的家庭Summary数据。
   *
   * @param familyId 家庭标识
   * @param familyName 家庭名称
   * @param merchantName 商户名称
   */
  public record FamilySummary(Long familyId, String familyName, String merchantName) {
  }

  /**
   * 封装返回给调用方的成员Summary数据。
   *
   * @param memberId 成员标识
   * @param name 名称
   * @param roleTemplate 角色Template
   */
  public record MemberSummary(Long memberId, String name, String roleTemplate) {
  }

  /**
   * 封装返回给调用方的Featured菜品数据。
   *
   * @param dishId 菜品标识
   * @param name 名称
   * @param price price
   * @param imageUrl imageUrl
   */
  public record FeaturedDish(Long dishId, String name, BigDecimal price, String imageUrl) {
  }

  /**
   * 封装返回给调用方的DashboardCard数据。
   *
   * @param key key
   * @param label label
   * @param value 取值
   */
  public record DashboardCard(String key, String label, String value) {
  }

  /**
   * 封装返回给调用方的MealSlot数据。
   *
   * @param mealSlotId mealSlot标识
   * @param name 名称
   * @param displayTime display时间
   * @param selected selected
   */
  public record MealSlotView(Long mealSlotId, String name, String displayTime, boolean selected) {
  }

  /**
   * 封装返回给调用方的订单Summary数据。
   *
   * @param orderId 订单标识
   * @param mealSlotName mealSlot名称
   * @param status 状态
   * @param totalAmount total金额
   */
  public record OrderSummary(Long orderId, String mealSlotName, String status, BigDecimal totalAmount) {
  }
}

