package com.familykitchen.family;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.service.impl.FamilyApplicationServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Deleted dishes may remain in historical data but cannot be opened by family clients. */
class FamilyDishDetailDeletionTest {

  @Test
  void familyDishDetailRejectsDeletedDish() {
    CurrentUserContext user = new CurrentUserContext(
        3L, 5L, 7L, 11L, "member", Set.of(), Set.of());
    DishMapper mapper = mock(DishMapper.class);
    DishEntity deleted = new DishEntity();
    deleted.setId(41L);
    deleted.setMerchantId(5L);
    deleted.setStatus("deleted");
    when(mapper.selectDish(5L, 41L)).thenReturn(deleted);
    FamilyApplicationServiceImpl service = new FamilyApplicationServiceImpl(
        mock(FamilyMapper.class), mapper, mock(ObjectMapper.class));

    assertThrows(BusinessException.class, () -> service.dishDetail(user, 41L));
  }
}
