package com.familykitchen.dish;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.dto.DishStatusRequest;
import com.familykitchen.dish.model.dto.DishMutationResult;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.system.service.SystemSettingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * 菜品更新事务编排测试，确保菜品主信息、食材配方和商户制作步骤在一次服务调用中同步更新。
 */
class DishApplicationServiceUpdateTest {

  @Test
  void dishDetailReturnsCompleteStructuredCookingStep() {
    DishMapper mapper = mock(DishMapper.class);
    DishEntity dish = new DishEntity(); dish.setId(8L); dish.setMerchantId(2L);
    DishCookingStepEntity step = new DishCookingStepEntity(); step.setStepNo(1); step.setTitle("焖煮");
    step.setContent("盖盖焖煮"); step.setDurationSeconds(900); step.setTemperatureText("保持微沸");
    step.setHeatLevel("小火"); step.setComponentTemplateId(21L);
    when(mapper.selectDish(2L, 8L)).thenReturn(dish);
    when(mapper.selectDishIngredients(8L)).thenReturn(List.of());
    when(mapper.selectCookingSteps(8L)).thenReturn(List.of(step));

    var detail = new DishApplicationServiceImpl(mapper, mock(SystemSettingService.class),
        mock(DishReviewService.class), mock(MerchantDishMutationLock.class), mock(FamilyMapper.class))
        .detail(new CurrentUserContext(1L, 2L, null, null, null,
            Set.of("MERCHANT_ADMIN"), Set.of()), 8L);

    assertEquals(900, detail.cookingSteps().get(0).durationSeconds());
    assertEquals("保持微沸", detail.cookingSteps().get(0).temperatureText());
    assertEquals("小火", detail.cookingSteps().get(0).heatLevel());
    assertEquals(21L, detail.cookingSteps().get(0).componentTemplateId());
  }

  @Test
  void activeDishCreationEnablesDishForEveryActiveMerchantFamily() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    FamilyMapper families = mock(FamilyMapper.class);
    when(settings.dishReviewEnabled()).thenReturn(false);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    doAnswer(invocation -> {
      DishEntity inserted = invocation.getArgument(0);
      inserted.setId(8L);
      return 1;
    }).when(mapper).insertDish(any());
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(), List.of(), "active");

    new DishApplicationServiceImpl(mapper, settings, mock(DishReviewService.class),
        mock(MerchantDishMutationLock.class), families).createDish(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), request);

    verify(families).enableDishForActiveFamilies(2L, 8L);
  }

  @Test
  void inactiveDishCreationDoesNotEnableFamilyMenus() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    FamilyMapper families = mock(FamilyMapper.class);
    when(settings.dishReviewEnabled()).thenReturn(false);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(), List.of(), "inactive");

    new DishApplicationServiceImpl(mapper, settings, mock(DishReviewService.class),
        mock(MerchantDishMutationLock.class), families).createDish(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), request);

    verify(families, never()).enableDishForActiveFamilies(any(), any());
  }

  @Test
  void pendingReviewCreationDoesNotEnableFamilyMenusBeforeApproval() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    DishReviewService reviews = mock(DishReviewService.class);
    FamilyMapper families = mock(FamilyMapper.class);
    when(settings.dishReviewEnabled()).thenReturn(true);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(), List.of(), "active");

    new DishApplicationServiceImpl(mapper, settings, reviews,
        mock(MerchantDishMutationLock.class), families).createDish(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), request);

    verify(reviews).submit(1L, 2L, null, request);
    verify(families, never()).enableDishForActiveFamilies(any(), any());
  }

  @Test
  void dishListExposesImportedTemplateSource() {
    DishMapper mapper = mock(DishMapper.class);
    DishEntity imported = new DishEntity();
    imported.setId(8L);
    imported.setMerchantId(2L);
    imported.setSourceTemplateId(5L);
    imported.setName("豆角焖面");
    imported.setBasePrice(BigDecimal.TEN);
    imported.setStatus("active");
    when(mapper.selectDishes(2L)).thenReturn(List.of(imported));

    DishView view = new DishApplicationServiceImpl(mapper, mock(SystemSettingService.class),
        mock(DishReviewService.class), mock(MerchantDishMutationLock.class), mock(FamilyMapper.class)).dishes(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of())).get(0);

    org.junit.jupiter.api.Assertions.assertEquals(5L, view.sourceTemplateId());
    org.junit.jupiter.api.Assertions.assertTrue(view.templateImported());
  }

  @Test
  void updateWithoutReviewReplacesIngredientsAndCookingStepsInOneServiceCall() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    when(lock.lock(2L, 8L)).thenReturn(existing);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    when(settings.dishReviewEnabled()).thenReturn(false);
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(new DishRequest.IngredientRequest("盐", BigDecimal.ONE, "克", "FIXED")),
        List.of(new DishRequest.CookingStepRequest(1, "蒸", "蒸熟", 600, "100摄氏度", "中火", 21L)),
        "active");
    CurrentUserContext user = new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

    DishMutationResult result = new DishApplicationServiceImpl(mapper, settings,
        mock(DishReviewService.class), lock, mock(FamilyMapper.class)).updateDish(user, 8L, request);

    InOrder order = inOrder(lock, mapper);
    order.verify(lock).lock(2L, 8L);
    order.verify(mapper).updateDish(any());
    verify(mapper).deleteDishIngredients(8L);
    verify(mapper).insertDishIngredient(any());
    verify(mapper).deleteCookingSteps(8L);
    ArgumentCaptor<com.familykitchen.dish.model.entity.DishCookingStepEntity> step =
        ArgumentCaptor.forClass(com.familykitchen.dish.model.entity.DishCookingStepEntity.class);
    verify(mapper).insertCookingStep(step.capture());
    assertEquals(600, step.getValue().getDurationSeconds());
    assertEquals("100摄氏度", step.getValue().getTemperatureText());
    assertEquals("中火", step.getValue().getHeatLevel());
    assertEquals(21L, step.getValue().getComponentTemplateId());
    org.junit.jupiter.api.Assertions.assertEquals(DishMutationResult.Outcome.APPLIED, result.outcome());
  }

  @Test
  void statusUpdateWithoutReviewTouchesOnlyOwnedStatusColumn() {
    DishMapper mapper = mock(DishMapper.class); SystemSettingService settings = mock(SystemSettingService.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    when(lock.lock(2L, 8L)).thenReturn(existing); when(settings.dishReviewEnabled()).thenReturn(false);
    when(mapper.setDishInactiveAndClearFeatured(2L, 8L)).thenReturn(1);
    DishReviewService reviews = mock(DishReviewService.class);
    DishMutationResult result = new DishApplicationServiceImpl(mapper, settings, reviews,
        lock, mock(FamilyMapper.class)).updateDishStatus(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), 8L,
        new DishStatusRequest("INACTIVE"));
    InOrder order = inOrder(lock, mapper);
    order.verify(lock).lock(2L, 8L);
    order.verify(mapper).setDishInactiveAndClearFeatured(2L, 8L);
    verify(mapper, never()).updateDish(any());
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
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    DishMutationResult result = new DishApplicationServiceImpl(mapper, settings, reviews,
        lock, mock(FamilyMapper.class)).updateDishStatus(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()), 8L,
        new DishStatusRequest("INACTIVE"));
    ArgumentCaptor<DishRequest> request = ArgumentCaptor.forClass(DishRequest.class);
    verify(reviews).submit(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(8L), request.capture());
    org.junit.jupiter.api.Assertions.assertEquals("inactive", request.getValue().status());
    org.junit.jupiter.api.Assertions.assertEquals(DishMutationResult.Outcome.PENDING_REVIEW, result.outcome());
    verify(mapper, never()).updateDishStatus(any(), any(), any());
    verify(mapper, never()).setDishInactiveAndClearFeatured(any(), any());
    verify(lock, never()).lock(any(), any());
  }

  @Test
  void activeStatusUpdateUsesMerchantThenDishLockBeforeWrite() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L);
    when(settings.dishReviewEnabled()).thenReturn(false);
    when(lock.lock(2L, 8L)).thenReturn(existing);
    when(mapper.updateDishStatus(2L, 8L, "active")).thenReturn(1);

    new DishApplicationServiceImpl(mapper, settings, mock(DishReviewService.class), lock,
        mock(FamilyMapper.class)).updateDishStatus(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()),
        8L, new DishStatusRequest("ACTIVE"));

    InOrder order = inOrder(lock, mapper);
    order.verify(lock).lock(2L, 8L);
    order.verify(mapper).updateDishStatus(2L, 8L, "active");
  }

  @Test
  void fullUpdateNormalizesWhitespaceAndCaseBeforeInactiveWrite() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    DishEntity existing = new DishEntity(); existing.setId(8L); existing.setMerchantId(2L);
    when(settings.dishReviewEnabled()).thenReturn(false);
    when(lock.lock(2L, 8L)).thenReturn(existing);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(), List.of(), " INACTIVE ");

    new DishApplicationServiceImpl(mapper, settings, mock(DishReviewService.class), lock,
        mock(FamilyMapper.class)).updateDish(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()),
        8L, request);

    ArgumentCaptor<DishEntity> updated = ArgumentCaptor.forClass(DishEntity.class);
    verify(mapper).updateDish(updated.capture());
    assertEquals("inactive", updated.getValue().getStatus());
  }

  @Test
  void fullUpdateRejectsInvalidNonBlankStatusWithoutWriting() {
    DishMapper mapper = mock(DishMapper.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    when(settings.dishReviewEnabled()).thenReturn(false);
    when(lock.lock(2L, 8L)).thenReturn(new DishEntity());
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(), List.of(), "paused");
    DishApplicationServiceImpl service = new DishApplicationServiceImpl(mapper, settings,
        mock(DishReviewService.class), lock, mock(FamilyMapper.class));

    BusinessException error = assertThrows(BusinessException.class, () -> service.updateDish(
        new CurrentUserContext(1L, 2L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of()),
        8L, request));

    assertEquals(ErrorCode.BAD_REQUEST, error.errorCode());
    verify(mapper, never()).updateDish(any());
    verify(mapper, never()).deleteDishIngredients(any());
    verify(mapper, never()).deleteCookingSteps(any());
  }
}
