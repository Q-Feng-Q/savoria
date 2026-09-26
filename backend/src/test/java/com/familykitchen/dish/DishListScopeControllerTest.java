package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.controller.DishController;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.system.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

/** Covers explicit available/deleted merchant dish list scopes. */
class DishListScopeControllerTest {

  private static final CurrentUserContext USER = new CurrentUserContext(
      7L, 9L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

  @Test
  void controllerDefaultsScopeToAvailableAndForwardsDeletedScope() throws Exception {
    Method method = DishController.class.getMethod("dishes", HttpServletRequest.class, String.class, String.class);
    RequestParam annotation = method.getParameters()[1].getAnnotation(RequestParam.class);
    assertEquals("available", annotation.defaultValue());

    CurrentUserProvider users = mock(CurrentUserProvider.class);
    DishApplicationService dishes = mock(DishApplicationService.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(users.require(request)).thenReturn(USER);
    when(dishes.dishes(USER, "deleted")).thenReturn(List.of());

    new DishController(users, dishes, mock(DishReviewService.class))
        .dishes(request, "deleted", null);

    verify(dishes).dishes(USER, "deleted");
  }

  @Test
  void serviceNormalizesBlankScopeAndRejectsUnknownScope() {
    DishMapper mapper = mock(DishMapper.class);
    DishApplicationServiceImpl service = service(mapper);
    when(mapper.selectDishes(9L, "available")).thenReturn(List.of());

    service.dishes(USER, "  ");
    verify(mapper).selectDishes(9L, "available");
    assertThrows(BusinessException.class, () -> service.dishes(USER, "archive"));
  }

  @Test
  void merchantDetailRejectsDeletedDish() {
    DishMapper mapper = mock(DishMapper.class);
    DishEntity deleted = new DishEntity();
    deleted.setId(31L);
    deleted.setMerchantId(9L);
    deleted.setStatus("deleted");
    when(mapper.selectDish(9L, 31L)).thenReturn(deleted);

    assertThrows(BusinessException.class, () -> service(mapper).detail(USER, 31L));
  }

  private static DishApplicationServiceImpl service(DishMapper mapper) {
    return new DishApplicationServiceImpl(mapper, mock(SystemSettingService.class),
        mock(DishReviewService.class), mock(MerchantDishMutationLock.class), mock(FamilyMapper.class));
  }
}
