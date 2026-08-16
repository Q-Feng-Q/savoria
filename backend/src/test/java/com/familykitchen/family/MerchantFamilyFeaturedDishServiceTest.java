package com.familykitchen.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.service.impl.MerchantFamilyMenuApplicationServiceImpl;
import com.familykitchen.dish.service.DishApplicationService;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies ownership and eligibility rules for merchant-selected family recommendations. */
class MerchantFamilyFeaturedDishServiceTest {
  private static final CurrentUserContext MERCHANT = new CurrentUserContext(
      2L, 2L, 1L, 2L, "owner", Set.of(), Set.of("MERCHANT_ADMIN"));

  @Test
  void oldEndpointValidatesFamilyThenAdaptsToMerchantWideRecommendation() {
    FamilyMapper mapper = mock(FamilyMapper.class);
    DishApplicationService dishes = mock(DishApplicationService.class);
    when(mapper.countFamilyOwnership(2L, 1L)).thenReturn(1);

    new MerchantFamilyMenuApplicationServiceImpl(mapper, dishes)
        .setFeaturedDish(MERCHANT, 1L, 8L);

    verify(dishes).setFeaturedDish(MERCHANT, 8L, true);
    verify(mapper, never()).updateFeaturedDish(2L, 1L, 8L);
  }

  @Test
  void rejectsForeignFamilyBeforeDishEligibilityCheck() {
    FamilyMapper mapper = mock(FamilyMapper.class);
    DishApplicationService dishes = mock(DishApplicationService.class);
    MerchantFamilyMenuApplicationServiceImpl service =
        new MerchantFamilyMenuApplicationServiceImpl(mapper, dishes);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.setFeaturedDish(MERCHANT, 99L, 8L));

    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(mapper, never()).updateFeaturedDish(2L, 99L, 8L);
    verify(dishes, never()).setFeaturedDish(MERCHANT, 8L, true);
  }
}
