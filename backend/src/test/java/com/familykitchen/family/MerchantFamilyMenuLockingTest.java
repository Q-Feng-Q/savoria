package com.familykitchen.family;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.CopyFamilyMenuRequest;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.service.impl.MerchantFamilyMenuApplicationServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Family menu writes share merchant->sorted dishes locking with dish deletion. */
class MerchantFamilyMenuLockingTest {

  private static final CurrentUserContext USER = new CurrentUserContext(
      17L, 29L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

  @Test
  void saveLocksMerchantThenSortedDistinctDishesBeforeReplacingRows() {
    FamilyMapper mapper = mock(FamilyMapper.class);
    when(mapper.countFamilyOwnership(29L, 31L)).thenReturn(1);
    when(mapper.selectOwnedDishesForUpdate(29L, List.of(2L, 7L)))
        .thenReturn(List.of(dish(2L), dish(7L)));
    MerchantFamilyMenuApplicationServiceImpl service = new MerchantFamilyMenuApplicationServiceImpl(
        mapper, mock(DishApplicationService.class));

    service.saveMenu(USER, 31L, new SaveFamilyMenuRequest(List.of(
        new SaveFamilyMenuRequest.MenuItem(7L, true, 2, BigDecimal.TEN),
        new SaveFamilyMenuRequest.MenuItem(2L, false, 1, BigDecimal.ONE))));

    InOrder order = inOrder(mapper);
    order.verify(mapper).lockMerchantForMenu(29L);
    order.verify(mapper).countFamilyOwnership(29L, 31L);
    order.verify(mapper).selectOwnedDishesForUpdate(29L, List.of(2L, 7L));
    order.verify(mapper).deleteFamilyMenu(31L);
  }

  @Test
  void copyLocksSourceDishSetBeforeBlindCopy() {
    FamilyMapper mapper = mock(FamilyMapper.class);
    when(mapper.countFamilyOwnership(29L, 31L)).thenReturn(1);
    when(mapper.countFamilyOwnership(29L, 32L)).thenReturn(1);
    when(mapper.selectFamilyMenuDishIds(32L)).thenReturn(List.of(7L, 2L, 7L));
    when(mapper.selectOwnedDishesForUpdate(29L, List.of(2L, 7L)))
        .thenReturn(List.of(dish(2L), dish(7L)));
    MerchantFamilyMenuApplicationServiceImpl service = new MerchantFamilyMenuApplicationServiceImpl(
        mapper, mock(DishApplicationService.class));

    service.copyMenu(USER, 31L, new CopyFamilyMenuRequest(32L));

    InOrder order = inOrder(mapper);
    order.verify(mapper).lockMerchantForMenu(29L);
    order.verify(mapper).selectFamilyMenuDishIds(32L);
    order.verify(mapper).selectOwnedDishesForUpdate(29L, List.of(2L, 7L));
    order.verify(mapper).deleteFamilyMenu(31L);
    order.verify(mapper).copyFamilyMenu(31L, 32L);
  }

  private static DishEntity dish(Long id) {
    DishEntity dish = new DishEntity();
    dish.setId(id);
    dish.setMerchantId(29L);
    dish.setStatus("active");
    return dish;
  }
}
