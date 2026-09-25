package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.entity.DishCategoryEntity;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.vo.DishTemplateImportResultView;
import com.familykitchen.dish.model.vo.DishTemplateDetailView;
import com.familykitchen.dish.service.impl.DishTemplateServiceImpl;
import com.familykitchen.dish.service.impl.DishTemplateProcurementReadinessEvaluatorImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 验证模板导入会创建独立商户菜品且重复导入保持幂等。 */
class DishTemplateServiceTest {
  private static final CurrentUserContext USER = new CurrentUserContext(
      7L, 11L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());

  @Test
  void detailReturnsEveryFieldNeededToBuildACompleteChangeSnapshot() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishTemplateEntity source = template(2L, "番茄炒蛋", "家常热菜");
    source.setSortOrder(17);
    source.setEnabled(true);
    source.setVersion(6L);
    source.setSourceType("COOK_LIKE_HOC");
    source.setProductType("NOURISHMENT");
    source.setNourishmentDescription("汤羹特点");
    when(templateMapper.selectTemplate(11L, 2L)).thenReturn(source);
    when(templateMapper.selectTemplateIngredients(2L)).thenReturn(List.of(ingredient(2L, "番茄")));
    when(templateMapper.selectTemplateCookingSteps(2L))
        .thenReturn(List.of(step(21L, 2L, 1, "炒制番茄")));

    DishTemplateDetailView detail = service(templateMapper, mock(DishMapper.class)).detail(USER, 2L);

    assertEquals(17, detail.sortOrder());
    assertTrue(detail.enabled());
    assertEquals(6L, detail.version());
    assertEquals(1, detail.cookingSteps().size());
    assertEquals(21L, detail.cookingSteps().get(0).getId());
    assertEquals("COOK_LIKE_HOC", detail.sourceType());
    assertTrue(detail.importable());
    assertEquals("NOURISHMENT", detail.productType());
    assertEquals("汤羹特点", detail.nourishmentDescription());
  }

  @Test
  void importCopiesSelectedTemplateCategoryDishIngredientAndDictionary() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    DishTemplateEntity template = template(2L, "番茄炒蛋", "家常热菜");
    template.setProductType("NOURISHMENT");
    template.setServingAdvice("温热食用");
    DishTemplateIngredientEntity ingredient = ingredient(2L, "番茄");
    DishCategoryEntity category = new DishCategoryEntity();
    category.setId(8L);
    when(templateMapper.selectTemplatesByIds(List.of(2L))).thenReturn(List.of(template));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of());
    when(templateMapper.selectEligibleTemplateForUpdate(2L)).thenReturn(template);
    when(templateMapper.selectTemplateIngredients(2L)).thenReturn(List.of(ingredient));
    var illustratedStep = step(21L, 2L, 1, "炒制");
    illustratedStep.setImageUrls(List.of("/uploads/images/a.jpg", "/uploads/images/b.png"));
    when(templateMapper.selectTemplateCookingSteps(2L)).thenReturn(List.of(illustratedStep));
    when(templateMapper.selectMerchantCategoryByName(11L, "家常热菜")).thenReturn(category);
    when(templateMapper.insertImportedDishIgnore(any())).thenAnswer(invocation -> {
      invocation.<com.familykitchen.dish.model.entity.DishEntity>getArgument(0).setId(99L);
      return 1;
    });

    DishTemplateImportResultView result = service(templateMapper, dishMapper)
        .importTemplates(USER, List.of(2L));

    assertEquals(List.of(2L), result.importedIds());
    assertEquals(1, result.importedCount());
    assertTrue(result.skippedIds().isEmpty());
    ArgumentCaptor<com.familykitchen.dish.model.entity.DishEntity> dishCaptor =
        ArgumentCaptor.forClass(com.familykitchen.dish.model.entity.DishEntity.class);
    verify(templateMapper).insertImportedDishIgnore(dishCaptor.capture());
    assertEquals(2L, dishCaptor.getValue().getSourceTemplateId());
    assertEquals("active", dishCaptor.getValue().getStatus());
    assertEquals("NOURISHMENT", dishCaptor.getValue().getProductType());
    assertEquals("温热食用", dishCaptor.getValue().getServingAdvice());
    verify(dishMapper).insertDishIngredient(any());
    var savedStep = ArgumentCaptor.forClass(DishCookingStepEntity.class);
    verify(dishMapper).insertCookingStep(savedStep.capture());
    assertEquals(illustratedStep.getImageUrls(), savedStep.getValue().getImageUrls());
    verify(templateMapper).insertMerchantIngredientIgnore(any());
  }

  @Test
  void importSkipsTemplateAlreadyOwnedByMerchant() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    when(templateMapper.selectTemplatesByIds(List.of(2L))).thenReturn(List.of(template(2L, "番茄炒蛋", "家常热菜")));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of(2L));
    DishTemplateImportResultView result = service(templateMapper, mock(DishMapper.class))
        .importTemplates(USER, List.of(2L, 2L));
    assertEquals(List.of(2L), result.skippedIds());
  }

  @Test
  void importAllProcessesEveryEligibleTemplateAndCopiesIngredients() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    List<DishTemplateEntity> templates = LongStream.rangeClosed(1, 240)
        .mapToObj(id -> template(id, "系统菜" + id, "家常热菜")).toList();
    DishCategoryEntity category = new DishCategoryEntity();
    category.setId(8L);
    when(templateMapper.selectAllEnabledTemplates()).thenReturn(templates);
    when(templateMapper.selectImportedTemplateIds(11L,
        LongStream.rangeClosed(1, 240).boxed().toList())).thenReturn(List.of(1L));
    when(templateMapper.selectEligibleTemplateForUpdate(anyLong())).thenAnswer(invocation ->
        templates.get(invocation.<Long>getArgument(0).intValue() - 1));
    when(templateMapper.selectTemplateIngredients(anyLong())).thenAnswer(invocation ->
        List.of(ingredient(invocation.getArgument(0), "番茄")));
    when(templateMapper.selectTemplateCookingSteps(anyLong())).thenReturn(List.of());
    when(templateMapper.selectMerchantCategoryByName(11L, "家常热菜")).thenReturn(category);
    when(templateMapper.insertImportedDishIgnore(any())).thenAnswer(invocation -> {
      com.familykitchen.dish.model.entity.DishEntity dish = invocation.getArgument(0);
      dish.setId(1000L + dish.getSourceTemplateId());
      return 1;
    });

    DishTemplateImportResultView result = service(templateMapper, dishMapper).importAllTemplates(USER);

    assertEquals(239, result.importedCount());
    assertEquals(1, result.skippedCount());
    assertEquals(List.of(1L), result.skippedIds());
    assertEquals(240L, result.importedIds().get(result.importedIds().size() - 1));
    verify(templateMapper, times(239)).insertImportedDishIgnore(any());
    verify(dishMapper, times(239)).insertDishIngredient(any());
    verify(templateMapper, times(239)).insertMerchantIngredientIgnore(any());
  }

  @Test
  void importAllReturnsEmptyWithoutCallingIdListQueries() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    when(templateMapper.selectAllEnabledTemplates()).thenReturn(List.of());

    DishTemplateImportResultView result = service(templateMapper, dishMapper).importAllTemplates(USER);

    assertEquals(0, result.importedCount());
    assertEquals(0, result.skippedCount());
    verify(templateMapper, never()).selectImportedTemplateIds(any(), any());
    verify(templateMapper, never()).selectTemplateIngredientsByIds(any());
    verifyNoInteractions(dishMapper);
  }

  @Test
  void importAllSkipsConcurrentDuplicateWithoutWritingIngredients() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    DishTemplateEntity source = template(2L, "番茄炒蛋", "家常热菜");
    DishCategoryEntity category = new DishCategoryEntity();
    category.setId(8L);
    when(templateMapper.selectAllEnabledTemplates()).thenReturn(List.of(source));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of());
    when(templateMapper.selectEligibleTemplateForUpdate(2L)).thenReturn(source);
    when(templateMapper.selectTemplateIngredients(2L)).thenReturn(List.of(ingredient(2L, "番茄")));
    when(templateMapper.selectTemplateCookingSteps(2L)).thenReturn(List.of());
    when(templateMapper.selectMerchantCategoryByName(11L, "家常热菜")).thenReturn(category);
    when(templateMapper.insertImportedDishIgnore(any())).thenReturn(0);

    DishTemplateImportResultView result = service(templateMapper, dishMapper).importAllTemplates(USER);

    assertEquals(List.of(2L), result.skippedIds());
    verifyNoInteractions(dishMapper);
    verify(templateMapper, never()).insertMerchantIngredientIgnore(any());
  }

  @Test
  void selectedImportKeepsValidationAndDeduplicationBoundaries() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    BusinessException empty = assertThrows(BusinessException.class,
        () -> service(templateMapper, dishMapper).importTemplates(USER, List.of()));
    assertEquals(ErrorCode.BAD_REQUEST, empty.errorCode());

    List<Long> tooMany = LongStream.rangeClosed(1, 101).boxed().toList();
    BusinessException overflow = assertThrows(BusinessException.class,
        () -> service(templateMapper, dishMapper).importTemplates(USER, tooMany));
    assertEquals(ErrorCode.BAD_REQUEST, overflow.errorCode());

    when(templateMapper.selectTemplatesByIds(List.of(2L))).thenReturn(List.of());
    BusinessException missing = assertThrows(BusinessException.class,
        () -> service(templateMapper, dishMapper).importTemplates(USER, List.of(2L, 2L)));
    assertEquals(ErrorCode.BUSINESS_INVALID, missing.errorCode());
    verify(templateMapper).selectTemplatesByIds(List.of(2L));
    verify(templateMapper, never()).insertImportedDishIgnore(any());
    verifyNoInteractions(dishMapper);
  }

  @Test
  void importRejectsTemplateThatBecomesIneligibleBeforeWrite() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    when(templateMapper.selectTemplatesByIds(List.of(2L)))
        .thenReturn(List.of(template(2L, "番茄炒蛋", "家常热菜")));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of());
    when(templateMapper.selectEligibleTemplateForUpdate(2L)).thenReturn(null);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service(templateMapper, dishMapper).importTemplates(USER, List.of(2L)));

    assertEquals(ErrorCode.BUSINESS_INVALID, error.errorCode());
    assertTrue(error.getMessage().contains("价格或采购数据已变化"));
    verify(templateMapper, never()).insertImportedDishIgnore(any());
  }

  @Test
  void importExpandsComponentIngredientsAndCopiesComponentStepsBeforeDishSteps() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    DishTemplateEntity dish = template(2L, "宫保鸡丁", "家常热菜");
    DishTemplateEntity sauce = template(9L, "宫保汁", "配料组件");
    sauce.setTemplateType("COMPONENT");
    DishTemplateIngredientEntity component = new DishTemplateIngredientEntity();
    component.setTemplateId(2L);
    component.setIngredientName("宫保汁");
    component.setQuantityStatus("NOT_APPLICABLE");
    component.setComponentTemplateId(9L);
    component.setComponentOccurrenceKey("宫保鸡丁.md#宫保汁-1");
    component.setComponentMultiplier(new BigDecimal("2"));
    DishTemplateIngredientEntity soy = ingredient(9L, "酱油");
    soy.setQuantity(new BigDecimal("20"));
    soy.setUnit("ml");
    DishTemplateCookingStepEntity sauceStep = step(91L, 9L, 1, "调宫保汁");
    DishTemplateCookingStepEntity dishStep = step(21L, 2L, 1, "炒制鸡丁");
    DishCategoryEntity category = new DishCategoryEntity();
    category.setId(8L);
    when(templateMapper.selectTemplatesByIds(List.of(2L))).thenReturn(List.of(dish));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of());
    when(templateMapper.selectEligibleTemplateForUpdate(2L)).thenReturn(dish);
    when(templateMapper.selectTemplateForUpdate(9L)).thenReturn(sauce);
    when(templateMapper.selectTemplateIngredients(2L)).thenReturn(List.of(component));
    when(templateMapper.selectTemplateIngredients(9L)).thenReturn(List.of(soy));
    when(templateMapper.selectTemplateCookingSteps(9L)).thenReturn(List.of(sauceStep));
    when(templateMapper.selectTemplateCookingSteps(2L)).thenReturn(List.of(dishStep));
    when(templateMapper.selectMerchantCategoryByName(11L, "家常热菜")).thenReturn(category);
    when(templateMapper.insertImportedDishIgnore(any())).thenAnswer(invocation -> {
      invocation.<com.familykitchen.dish.model.entity.DishEntity>getArgument(0).setId(99L);
      return 1;
    });

    service(templateMapper, dishMapper).importTemplates(USER, List.of(2L));

    ArgumentCaptor<com.familykitchen.dish.model.entity.DishIngredientEntity> ingredientCaptor =
        ArgumentCaptor.forClass(com.familykitchen.dish.model.entity.DishIngredientEntity.class);
    verify(dishMapper).insertDishIngredient(ingredientCaptor.capture());
    assertEquals(new BigDecimal("40"), ingredientCaptor.getValue().getQuantity());
    assertEquals("ml", ingredientCaptor.getValue().getUnit());
    ArgumentCaptor<DishCookingStepEntity> stepCaptor = ArgumentCaptor.forClass(DishCookingStepEntity.class);
    verify(dishMapper, times(2)).insertCookingStep(stepCaptor.capture());
    assertEquals(List.of(1, 2), stepCaptor.getAllValues().stream().map(DishCookingStepEntity::getStepNo).toList());
    assertEquals("宫保汁", stepCaptor.getAllValues().get(0).getTitle());
    assertEquals(9L, stepCaptor.getAllValues().get(0).getComponentTemplateId());
    assertEquals(91L, stepCaptor.getAllValues().get(0).getSourceTemplateStepId());
    assertEquals(21L, stepCaptor.getAllValues().get(1).getSourceTemplateStepId());
  }

  private static DishTemplateServiceImpl service(DishTemplateMapper templateMapper, DishMapper dishMapper) {
    return new DishTemplateServiceImpl(templateMapper, dishMapper, new ObjectMapper(),
        new DishTemplateProcurementReadinessEvaluatorImpl());
  }

  private static DishTemplateEntity template(Long id, String name, String categoryName) {
    DishTemplateEntity value = new DishTemplateEntity();
    value.setId(id); value.setName(name); value.setCategoryName(categoryName);
    value.setDescription("家常菜"); value.setImageUrl("/images/dish-templates/dish-002.jpg");
    value.setReferencePrice(BigDecimal.valueOf(18)); value.setTasteTags("[\"家常\"]");
    value.setMealTags("[\"LUNCH\"]");
    value.setTemplateType("DISH"); value.setDataStatus("READY");
    value.setProcurementReady(true); value.setEnabled(true);
    return value;
  }

  private static DishTemplateIngredientEntity ingredient(Long templateId, String name) {
    DishTemplateIngredientEntity value = new DishTemplateIngredientEntity();
    value.setTemplateId(templateId); value.setIngredientName(name); value.setIngredientCategory("蔬菜及其他");
    value.setQuantity(BigDecimal.valueOf(200)); value.setUnit("g"); value.setCalcType("FIXED");
    value.setQuantityStatus("VERIFIED");
    return value;
  }

  private static DishTemplateCookingStepEntity step(Long id, Long templateId, int stepNo,
      String content) {
    DishTemplateCookingStepEntity value = new DishTemplateCookingStepEntity();
    value.setId(id); value.setTemplateId(templateId); value.setStepNo(stepNo);
    value.setTitle(content); value.setContent(content);
    return value;
  }
}
