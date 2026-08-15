package com.familykitchen.dish;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.dto.DishStatusRequest;
import com.familykitchen.dish.model.dto.DishMutationResult;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.system.service.SystemSettingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 菜品更新事务编排测试，确保菜品主信息、食材配方和商户制作步骤在一次服务调用中同步更新。
 */
class DishApplicationServiceUpdateTest {
  @Test
  void updateWithoutReviewReplacesIngredientsAndCookingStepsInOneServiceCall() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L);
    when(mapper.selectDish(2L, 8L)).thenReturn(existing);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    when(settings.dishReviewEnabled()).thenReturn(false);
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(new DishRequest.IngredientRequest("盐", BigDecimal.ONE, "克", "FIXED")),
        List.of(new DishRequest.CookingStepRequest(1, "蒸", "蒸熟")), "active");
    CurrentUserContext user = new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

    DishMutationResult result = new DishApplicationServiceImpl(mapper, settings, mock(DishReviewService.class)).updateDish(user, 8L, request);

    verify(mapper).deleteDishIngredients(8L);
    verify(mapper).insertDishIngredient(any());
    verify(mapper).deleteCookingSteps(8L);
    verify(mapper).insertCookingStep(any());
    org.junit.jupiter.api.Assertions.assertEquals(DishMutationResult.Outcome.APPLIED, result.outcome());
  }

  @Test
  void statusUpdateWithoutReviewTouchesOnlyOwnedStatusColumn() {
    DishMapper mapper = mock(DishMapper.class); SystemSettingService settings = mock(SystemSettingService.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L);
    when(mapper.selectDish(2L, 8L)).thenReturn(existing); when(settings.dishReviewEnabled()).thenReturn(false);
    when(mapper.updateDishStatus(2L, 8L, "inactive")).thenReturn(1);
    DishReviewService reviews = mock(DishReviewService.class);
    DishMutationResult result = new DishApplicationServiceImpl(mapper, settings, reviews).updateDishStatus(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), 8L,
        new DishStatusRequest("INACTIVE"));
    verify(mapper).updateDishStatus(2L, 8L, "inactive"); verify(mapper, never()).updateDish(any());
    org.junit.jupiter.api.Assertions.assertEquals(DishMutationResult.Outcome.APPLIED, result.outcome());
  }

  @Test
  void statusUpdateWithReviewSubmitsFullSnapshotWithoutDirectMutation() {
    DishMapper mapper = mock(DishMapper.class); SystemSettingService settings = mock(SystemSettingService.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L); existing.setCategoryId(3L);
    existing.setName("鱼"); existing.setBasePrice(BigDecimal.TEN); existing.setStatus("active");
    when(mapper.selectDish(2L, 8L)).thenReturn(existing); when(settings.dishReviewEnabled()).thenReturn(true);
    when(mapper.selectDishIngredients(8L)).thenReturn(List.of()); when(mapper.selectCookingSteps(8L)).thenReturn(List.of());
    DishReviewService reviews = mock(DishReviewService.class);
    DishMutationResult result = new DishApplicationServiceImpl(mapper, settings, reviews).updateDishStatus(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), 8L,
        new DishStatusRequest("INACTIVE"));
    ArgumentCaptor<DishRequest> request = ArgumentCaptor.forClass(DishRequest.class);
    verify(reviews).submit(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(8L), request.capture());
    org.junit.jupiter.api.Assertions.assertEquals("inactive", request.getValue().status());
    org.junit.jupiter.api.Assertions.assertEquals(DishMutationResult.Outcome.PENDING_REVIEW, result.outcome());
    verify(mapper, never()).updateDishStatus(any(), any(), any());
  }
}
