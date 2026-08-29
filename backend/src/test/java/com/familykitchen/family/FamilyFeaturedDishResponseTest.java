package com.familykitchen.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.service.impl.FamilyApplicationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies the family home multi-recommendation response and dish projection.
 */
class FamilyFeaturedDishResponseTest {

  private static final CurrentUserContext USER = new CurrentUserContext(
    3L, 5L, 7L, 11L, "member", Set.of(), Set.of());

  private FamilyMapper familyMapper;
  private FamilyApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    familyMapper = mock(FamilyMapper.class);
    service = new FamilyApplicationServiceImpl(
      familyMapper, mock(DishMapper.class), mock(ObjectMapper.class));

    FamilyRecord family = new FamilyRecord();
    family.setFamilyId(USER.familyId());
    family.setFamilyName("测试家庭");
    family.setMerchantName("测试厨房");
    family.setActiveMenuCount(4);
    family.setAddressCount(2);
    FamilyMemberRecord member = new FamilyMemberRecord();
    member.setMemberId(USER.memberId());
    member.setName("成员");
    member.setRoleTemplate("member");
    when(familyMapper.selectFamily(USER.merchantId(), USER.familyId())).thenReturn(family);
    when(familyMapper.selectMember(USER.memberId())).thenReturn(member);
    when(familyMapper.selectRecentOrders(USER.familyId())).thenReturn(List.of());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void homeReturnsOneToFiveRecommendationsAndKeepsFirstCompatibilityField(int count) {
    List<FamilyHomeResponse.FeaturedDish> recommendations = LongStream.rangeClosed(1, count)
      .mapToObj(FamilyFeaturedDishResponseTest::dish)
      .toList();
    when(familyMapper.selectFeaturedDishes(USER.familyId(), 5)).thenReturn(recommendations);

    FamilyHomeResponse response = service.home(USER);

    assertSame(recommendations, response.featuredDishes());
    assertSame(recommendations.get(0), response.featuredDish());
    verify(familyMapper).selectFeaturedDishes(USER.familyId(), 5);
    verify(familyMapper, never()).selectFallbackDish(USER.familyId());
  }

  @Test
  void homeUsesOneFallbackDishWhenNoMerchantRecommendationExists() {
    FamilyHomeResponse.FeaturedDish fallback = dish(21L);
    when(familyMapper.selectFeaturedDishes(USER.familyId(), 5)).thenReturn(List.of());
    when(familyMapper.selectFallbackDish(USER.familyId())).thenReturn(fallback);

    FamilyHomeResponse response = service.home(USER);

    assertEquals(List.of(fallback), response.featuredDishes());
    assertSame(fallback, response.featuredDish());
    verify(familyMapper).selectFallbackDish(USER.familyId());
  }

  @Test
  void homeReturnsEmptyListAndNullCompatibilityFieldWhenThereIsNoValidDish() {
    when(familyMapper.selectFeaturedDishes(USER.familyId(), 5)).thenReturn(null);
    when(familyMapper.selectFallbackDish(USER.familyId())).thenReturn(null);

    FamilyHomeResponse response = service.home(USER);

    assertEquals(List.of(), response.featuredDishes());
    assertNull(response.featuredDish());
  }

  @Test
  void homeUsesFixedQueryCountWithoutPerRecommendationLookup() {
    when(familyMapper.selectFeaturedDishes(USER.familyId(), 5))
      .thenReturn(List.of(dish(15L), dish(14L), dish(13L), dish(12L), dish(11L)));

    service.home(USER);

    verify(familyMapper).selectFamily(USER.merchantId(), USER.familyId());
    verify(familyMapper).selectMember(USER.memberId());
    verify(familyMapper).selectMembers(USER.familyId());
    verify(familyMapper).selectFeaturedDishes(USER.familyId(), 5);
    verify(familyMapper).selectRecentOrders(USER.familyId());
    verifyNoMoreInteractions(familyMapper);
  }

  @Test
  void menuItemsCarryMerchantRecommendationStatusAndTimestampIntoDishView() {
    LocalDateTime featuredAt = LocalDateTime.of(2026, 8, 15, 12, 30);
    when(familyMapper.selectFamilyMenuItems(USER.merchantId(), USER.familyId())).thenReturn(List.of(
      new FamilyMenuItemView(31L, 2L, "推荐菜", "简介", "/dish.png",
        new BigDecimal("18.00"), new BigDecimal("16.00"), true, 1,
        featuredAt, true),
      new FamilyMenuItemView(32L, 2L, "已禁用菜", "简介", "/disabled.png",
        new BigDecimal("20.00"), null, false, 2, null, false)));

    List<DishView> dishes = service.menuItems(USER, null, null);

    assertEquals(1, dishes.size());
    assertTrue(dishes.get(0).featured());
    assertEquals(featuredAt, dishes.get(0).featuredAt());
    assertEquals(new BigDecimal("16.00"), dishes.get(0).price());
  }

  private static FamilyHomeResponse.FeaturedDish dish(Long id) {
    return new FamilyHomeResponse.FeaturedDish(
      id, "菜品" + id, "简介", new BigDecimal("12.00"), "/dish.png");
  }
}
