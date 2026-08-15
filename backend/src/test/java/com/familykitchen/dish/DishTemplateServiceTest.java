package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import com.familykitchen.dish.model.vo.DishTemplateImportResultView;
import com.familykitchen.dish.service.impl.DishTemplateServiceImpl;
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
  void importCopiesSelectedTemplateCategoryDishIngredientAndDictionary() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    DishTemplateEntity template = template(2L, "番茄炒蛋", "家常热菜");
    DishTemplateIngredientEntity ingredient = ingredient(2L, "番茄");
    DishCategoryEntity category = new DishCategoryEntity();
    category.setId(8L);
    when(templateMapper.selectTemplatesByIds(List.of(2L))).thenReturn(List.of(template));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of());
    when(templateMapper.selectTemplateIngredientsByIds(List.of(2L))).thenReturn(List.of(ingredient));
    when(templateMapper.selectMerchantCategoryByName(11L, "家常热菜")).thenReturn(category);
    when(templateMapper.insertImportedDishIgnore(any())).thenAnswer(invocation -> {
      invocation.<com.familykitchen.dish.model.entity.DishEntity>getArgument(0).setId(99L);
      return 1;
    });

    DishTemplateImportResultView result = new DishTemplateServiceImpl(
        templateMapper, dishMapper, new ObjectMapper()).importTemplates(USER, List.of(2L));

    assertEquals(List.of(2L), result.importedIds());
    assertEquals(1, result.importedCount());
    assertTrue(result.skippedIds().isEmpty());
    ArgumentCaptor<com.familykitchen.dish.model.entity.DishEntity> dishCaptor =
        ArgumentCaptor.forClass(com.familykitchen.dish.model.entity.DishEntity.class);
    verify(templateMapper).insertImportedDishIgnore(dishCaptor.capture());
    assertEquals(2L, dishCaptor.getValue().getSourceTemplateId());
    assertEquals("active", dishCaptor.getValue().getStatus());
    verify(dishMapper).insertDishIngredient(any());
    verify(templateMapper).insertMerchantIngredientIgnore(any());
  }

  @Test
  void importSkipsTemplateAlreadyOwnedByMerchant() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    when(templateMapper.selectTemplatesByIds(List.of(2L))).thenReturn(List.of(template(2L, "番茄炒蛋", "家常热菜")));
    when(templateMapper.selectImportedTemplateIds(11L, List.of(2L))).thenReturn(List.of(2L));
    DishTemplateImportResultView result = new DishTemplateServiceImpl(
        templateMapper, mock(DishMapper.class), new ObjectMapper()).importTemplates(USER, List.of(2L, 2L));
    assertEquals(List.of(2L), result.skippedIds());
  }

  @Test
  void importAllProcessesExactlyOneHundredNinetyEightTemplatesAndCopiesIngredients() {
    DishTemplateMapper templateMapper = mock(DishTemplateMapper.class);
    DishMapper dishMapper = mock(DishMapper.class);
    List<DishTemplateEntity> templates = LongStream.rangeClosed(1, 198)
        .mapToObj(id -> template(id, "系统菜" + id, "家常热菜")).toList();
    DishCategoryEntity category = new DishCategoryEntity();
    category.setId(8L);
    when(templateMapper.selectAllEnabledTemplates()).thenReturn(templates);
    when(templateMapper.selectImportedTemplateIds(11L,
        LongStream.rangeClosed(1, 198).boxed().toList())).thenReturn(List.of(1L));
    when(templateMapper.selectTemplateIngredientsByIds(any())).thenReturn(List.of(ingredient(2L, "番茄")));
    when(templateMapper.selectMerchantCategoryByName(11L, "家常热菜")).thenReturn(category);
    when(templateMapper.insertImportedDishIgnore(any())).thenAnswer(invocation -> {
      com.familykitchen.dish.model.entity.DishEntity dish = invocation.getArgument(0);
      dish.setId(1000L + dish.getSourceTemplateId());
      return 1;
    });

    DishTemplateImportResultView result = service(templateMapper, dishMapper).importAllTemplates(USER);

    assertEquals(197, result.importedCount());
    assertEquals(1, result.skippedCount());
    assertEquals(List.of(1L), result.skippedIds());
    assertEquals(198L, result.importedIds().get(result.importedIds().size() - 1));
    verify(templateMapper, times(197)).insertImportedDishIgnore(any());
    verify(dishMapper).insertDishIngredient(any());
    verify(templateMapper).insertMerchantIngredientIgnore(any());
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
    when(templateMapper.selectTemplateIngredientsByIds(List.of(2L))).thenReturn(List.of(ingredient(2L, "番茄")));
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
    assertEquals(ErrorCode.NOT_FOUND, missing.errorCode());
    verify(templateMapper).selectTemplatesByIds(List.of(2L));
    verify(templateMapper, never()).insertImportedDishIgnore(any());
    verifyNoInteractions(dishMapper);
  }

  private static DishTemplateServiceImpl service(DishTemplateMapper templateMapper, DishMapper dishMapper) {
    return new DishTemplateServiceImpl(templateMapper, dishMapper, new ObjectMapper());
  }

  private static DishTemplateEntity template(Long id, String name, String categoryName) {
    DishTemplateEntity value = new DishTemplateEntity();
    value.setId(id); value.setName(name); value.setCategoryName(categoryName);
    value.setDescription("家常菜"); value.setImageUrl("/images/dish-templates/dish-002.jpg");
    value.setReferencePrice(BigDecimal.valueOf(18)); value.setTasteTags("[\"家常\"]");
    value.setMealTags("[\"LUNCH\"]");
    return value;
  }

  private static DishTemplateIngredientEntity ingredient(Long templateId, String name) {
    DishTemplateIngredientEntity value = new DishTemplateIngredientEntity();
    value.setTemplateId(templateId); value.setIngredientName(name); value.setIngredientCategory("蔬菜及其他");
    value.setQuantity(BigDecimal.valueOf(200)); value.setUnit("g"); value.setCalcType("FIXED");
    return value;
  }
}
