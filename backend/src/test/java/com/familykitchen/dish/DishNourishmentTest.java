package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishApplicationServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.system.service.SystemSettingService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DishNourishmentTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private final DishMapper mapper = mock(DishMapper.class);
  private final MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
  private final CurrentUserContext user = new CurrentUserContext(1L, 2L, null, null, null,
      Set.of("MERCHANT_ADMIN"), Set.of());
  private final DishApplicationServiceImpl service = new DishApplicationServiceImpl(mapper,
      mock(SystemSettingService.class), mock(DishReviewService.class), lock, mock(FamilyMapper.class));

  private DishRequest request(String fields) throws Exception {
    return json.readValue("{\"name\":\"汤\",\"categoryId\":3,\"basePrice\":10,\"status\":\"active\"" + fields + "}", DishRequest.class);
  }

  @Test void createNormalizesAndReturnsNourishment() throws Exception {
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    var result = json.valueToTree(service.createDish(user, request(
        ",\"productType\":\"NOURISHMENT\",\"nourishmentDescription\":\"  食品\\n特点  \",\"servingAdvice\":\" 温热食用 \",\"precautions\":\"   \"")));
    assertEquals("NOURISHMENT", result.path("productType").asText());
    assertEquals("食品\n特点", result.path("nourishmentDescription").asText());
    assertEquals("温热食用", result.path("servingAdvice").asText());
    assertTrue(result.path("precautions").isNull());
  }

  @Test void legacyCreateDefaultsToNormal() throws Exception {
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    assertEquals("NORMAL", json.valueToTree(service.createDish(user, request(""))).path("productType").asText());
  }

  @Test void legacyUpdatePreservesAndExplicitBlankClears() throws Exception {
    DishEntity existing = json.readValue("{\"id\":8,\"merchantId\":2,\"productType\":\"NOURISHMENT\",\"nourishmentDescription\":\"原介绍\",\"servingAdvice\":\"原建议\"}", DishEntity.class);
    when(lock.lock(2L, 8L)).thenReturn(existing);
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    service.updateDish(user, 8L, request(",\"servingAdvice\":\"  \""));
    var saved = ArgumentCaptor.forClass(DishEntity.class);
    verify(mapper).updateDish(saved.capture());
    var result = json.valueToTree(saved.getValue());
    assertEquals("NOURISHMENT", result.path("productType").asText());
    assertEquals("原介绍", result.path("nourishmentDescription").asText());
    assertTrue(result.path("servingAdvice").isNull());
  }

  @Test void rejectsInvalidTypeAndOverlongTextWithoutInsert() throws Exception {
    when(mapper.countCategoryOwnership(2L, 3L)).thenReturn(1);
    assertThrows(Exception.class, () -> service.createDish(user, request(",\"productType\":\"INVALID\"")));
    assertThrows(Exception.class, () -> service.createDish(user, request(",\"nourishmentDescription\":\"" + "字".repeat(1001) + "\"")));
    verify(mapper, never()).insertDish(any());
  }
}
