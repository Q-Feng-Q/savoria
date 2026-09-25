package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.BatchDishMutationRequest;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.vo.BatchDishMutationResult;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.system.service.SystemSettingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Exact transactional semantics for merchant batch delete and restore. */
class DishBulkMutationServiceTest {

  private static final CurrentUserContext USER = new CurrentUserContext(
      17L, 29L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

  @Test
  void rejectsInvalidRawListsBeforeAnyLockOrWrite() {
    Fixture fixture = new Fixture();
    List<Long> tooMany = new ArrayList<>();
    for (long id = 1; id <= 101; id++) tooMany.add(id);

    assertThrows(BusinessException.class, () -> fixture.service.bulkDelete(USER, new BatchDishMutationRequest(List.of())));
    assertThrows(BusinessException.class, () -> fixture.service.bulkDelete(USER, new BatchDishMutationRequest(tooMany)));
    assertThrows(BusinessException.class, () -> fixture.service.bulkDelete(USER, new BatchDishMutationRequest(List.of(1L, -2L))));
    assertThrows(BusinessException.class, () -> fixture.service.bulkDelete(USER, new BatchDishMutationRequest(null)));
    verify(fixture.mapper, never()).lockMerchant(29L);
  }

  @Test
  void deleteDeduplicatesThenLocksSortedAndDisablesEveryValidatedMenuTarget() {
    Fixture fixture = new Fixture();
    when(fixture.mapper.selectDishesForUpdate(29L, List.of(2L, 7L)))
        .thenReturn(List.of(dish(2L, "deleted"), dish(7L, "active")));
    when(fixture.mapper.markDishesDeleted(29L, List.of(7L), 17L)).thenReturn(1);

    BatchDishMutationResult result = fixture.service.bulkDelete(
        USER, new BatchDishMutationRequest(List.of(7L, 2L, 7L)));

    assertEquals(new BatchDishMutationResult(3, 2, 1, 1), result);
    InOrder order = inOrder(fixture.mapper, fixture.familyMapper);
    order.verify(fixture.mapper).lockMerchant(29L);
    order.verify(fixture.mapper).selectDishesForUpdate(29L, List.of(2L, 7L));
    order.verify(fixture.mapper).markDishesDeleted(29L, List.of(7L), 17L);
    order.verify(fixture.familyMapper).disableDishesForMerchantFamilies(29L, List.of(7L, 2L));
    verify(fixture.reviewService).rejectPendingForDeletedDishes(29L, List.of(7L, 2L), 17L);
  }

  @Test
  void missingOrForeignDishAbortsWholeBatchBeforeWrites() {
    Fixture fixture = new Fixture();
    when(fixture.mapper.selectDishesForUpdate(29L, List.of(2L, 7L)))
        .thenReturn(List.of(dish(2L, "active")));

    assertThrows(BusinessException.class, () -> fixture.service.bulkDelete(
        USER, new BatchDishMutationRequest(List.of(7L, 2L))));

    verify(fixture.mapper, never()).markDishesDeleted(eq(29L), anyList(), eq(17L));
    verify(fixture.familyMapper, never()).disableDishesForMerchantFamilies(eq(29L), anyList());
  }

  @Test
  void restoreOnlyChangesDeletedRowsAndNeverReenablesFamilyMenus() {
    Fixture fixture = new Fixture();
    when(fixture.mapper.selectDishesForUpdate(29L, List.of(2L, 7L)))
        .thenReturn(List.of(dish(2L, "deleted"), dish(7L, "inactive")));
    when(fixture.mapper.restoreDeletedDishes(29L, List.of(2L))).thenReturn(1);

    BatchDishMutationResult result = fixture.service.bulkRestore(
        USER, new BatchDishMutationRequest(List.of(7L, 2L)));

    assertEquals(new BatchDishMutationResult(2, 2, 1, 1), result);
    verify(fixture.mapper).restoreDeletedDishes(29L, List.of(2L));
    verify(fixture.familyMapper, never()).enableDishForActiveFamilies(eq(29L), eq(2L));
  }

  private static DishEntity dish(Long id, String status) {
    DishEntity dish = new DishEntity();
    dish.setId(id);
    dish.setMerchantId(29L);
    dish.setStatus(status);
    return dish;
  }

  /** Provides isolated mapper and service mocks for each bulk-mutation scenario. */
  private static final class Fixture {
    private final DishMapper mapper = mock(DishMapper.class);
    private final FamilyMapper familyMapper = mock(FamilyMapper.class);
    private final DishReviewService reviewService = mock(DishReviewService.class);
    private final DishApplicationServiceImpl service = new DishApplicationServiceImpl(
        mapper, mock(SystemSettingService.class), reviewService,
        mock(MerchantDishMutationLock.class), familyMapper);
  }
}
