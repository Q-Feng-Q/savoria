package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.system.service.SystemSettingService;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Merchant-wide featured dish mutation contract. */
class MerchantFeaturedDishServiceTest {
  private static final CurrentUserContext MERCHANT = new CurrentUserContext(
      9L, 2L, null, null, "owner", Set.of(), Set.of("MERCHANT_ADMIN"));

  @Test
  void sharedLockAlwaysLocksMerchantBeforeOwnedDish() {
    DishMapper mapper = mock(DishMapper.class);
    DishEntity dish = dish(8L, 2L, "active", null);
    when(mapper.selectDishForUpdate(2L, 8L)).thenReturn(dish);

    DishEntity result = new MerchantDishMutationLock(mapper).lock(2L, 8L);

    assertSame(dish, result);
    InOrder order = inOrder(mapper);
    order.verify(mapper).lockMerchant(2L);
    order.verify(mapper).selectDishForUpdate(2L, 8L);
  }

  @Test
  void sharedLockReportsForeignOrMissingDishAsNotFound() {
    DishMapper mapper = mock(DishMapper.class);

    BusinessException error = assertThrows(BusinessException.class,
        () -> new MerchantDishMutationLock(mapper).lock(2L, 99L));

    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
  }

  @Test
  void recommendsActiveDishAndEnablesItForEveryActiveFamily() {
    Fixture fixture = fixture(dish(8L, 2L, "active", null));
    when(fixture.dishes.countActiveFeaturedDishes(2L)).thenReturn(4);

    fixture.service.setFeaturedDish(MERCHANT, 8L, true);

    InOrder order = inOrder(fixture.lock, fixture.dishes, fixture.families);
    order.verify(fixture.lock).lock(2L, 8L);
    order.verify(fixture.dishes).countActiveFeaturedDishes(2L);
    order.verify(fixture.dishes).setDishFeaturedAt(2L, 8L);
    order.verify(fixture.families).enableDishForActiveFamilies(2L, 8L);
  }

  @Test
  void rejectsSixthFeaturedDishWithoutWriting() {
    Fixture fixture = fixture(dish(8L, 2L, "active", null));
    when(fixture.dishes.countActiveFeaturedDishes(2L)).thenReturn(5);

    BusinessException error = assertThrows(BusinessException.class,
        () -> fixture.service.setFeaturedDish(MERCHANT, 8L, true));

    assertEquals(ErrorCode.BUSINESS_INVALID, error.errorCode());
    verify(fixture.dishes, never()).setDishFeaturedAt(2L, 8L);
    verify(fixture.families, never()).enableDishForActiveFamilies(2L, 8L);
  }

  @Test
  void duplicateRecommendationKeepsTimestampAndDoesNotTouchCountOrMenus() {
    LocalDateTime original = LocalDateTime.of(2026, 8, 15, 12, 0, 0, 123456000);
    Fixture fixture = fixture(dish(8L, 2L, "active", original));

    fixture.service.setFeaturedDish(MERCHANT, 8L, true);

    verify(fixture.dishes, never()).countActiveFeaturedDishes(2L);
    verify(fixture.dishes, never()).setDishFeaturedAt(2L, 8L);
    verify(fixture.families, never()).enableDishForActiveFamilies(2L, 8L);
  }

  @Test
  void duplicateCancellationSucceedsWithoutCountingOrTouchingMenus() {
    Fixture fixture = fixture(dish(8L, 2L, "inactive", null));

    fixture.service.setFeaturedDish(MERCHANT, 8L, false);
    fixture.service.setFeaturedDish(MERCHANT, 8L, false);

    verify(fixture.dishes, org.mockito.Mockito.times(2)).clearDishFeatured(2L, 8L);
    verify(fixture.dishes, never()).countActiveFeaturedDishes(2L);
    verify(fixture.families, never()).enableDishForActiveFamilies(2L, 8L);
  }

  @Test
  void rejectsInactiveDishBeforeCountOrWrites() {
    Fixture fixture = fixture(dish(8L, 2L, "inactive", null));

    BusinessException error = assertThrows(BusinessException.class,
        () -> fixture.service.setFeaturedDish(MERCHANT, 8L, true));

    assertEquals(ErrorCode.BUSINESS_INVALID, error.errorCode());
    verify(fixture.dishes, never()).countActiveFeaturedDishes(2L);
    verify(fixture.families, never()).enableDishForActiveFamilies(2L, 8L);
  }

  private static Fixture fixture(DishEntity locked) {
    DishMapper dishes = mock(DishMapper.class);
    FamilyMapper families = mock(FamilyMapper.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    when(lock.lock(2L, 8L)).thenReturn(locked);
    DishApplicationServiceImpl service = new DishApplicationServiceImpl(dishes,
        mock(SystemSettingService.class), mock(DishReviewService.class), lock, families);
    return new Fixture(dishes, families, lock, service);
  }

  private static DishEntity dish(Long id, Long merchantId, String status, LocalDateTime featuredAt) {
    DishEntity dish = new DishEntity();
    dish.setId(id);
    dish.setMerchantId(merchantId);
    dish.setStatus(status);
    dish.setFeaturedAt(featuredAt);
    return dish;
  }

  /**
   * Collaborators for a featured-dish service test.
   * @param dishes dish mapper
   * @param families family mapper
   * @param lock mutation lock
   * @param service service under test
   */
  private record Fixture(DishMapper dishes, FamilyMapper families,
                         MerchantDishMutationLock lock, DishApplicationServiceImpl service) {}
}
