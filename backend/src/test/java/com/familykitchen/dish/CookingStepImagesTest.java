package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.*;

import com.familykitchen.dish.model.dto.DishRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishReviewMapper;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishReviewServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.system.mapper.SystemAuditMapper;
import java.math.BigDecimal;

class CookingStepImagesTest {
  private final DishMapper dishes = mock(DishMapper.class);
  private final DishReviewMapper reviews = mock(DishReviewMapper.class);
  private final MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
  private final ObjectMapper json = new ObjectMapper()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private DishReviewServiceImpl service() {
    when(lock.lock(2L, 8L)).thenReturn(new DishEntity());
    return new DishReviewServiceImpl(reviews, dishes, json, mock(SystemAuditMapper.class), lock, mock(FamilyMapper.class));
  }
  private DishRequest request(List<DishRequest.CookingStepRequest> steps) {
    return new DishRequest("汤", 3L, null, null, BigDecimal.TEN, List.of(), steps, "inactive");
  }
  private DishCookingStepEntity existing() throws Exception {
    return json.readValue("{\"stepNo\":1,\"content\":\"cook\",\"imageUrls\":[\"/uploads/images/old.jpg\"]}", DishCookingStepEntity.class);
  }
  @Test void zeroAndFiveImagesAreAcceptedInOrder() {
    for (List<String> images : List.of(List.<String>of(), List.of("/uploads/images/1.jpg", "/images/2.png",
        "https://example.com/3.webp", "/uploads/dish-template-assets/4.jpg", "/uploads/images/5.png"))) {
      assertEquals(images, com.familykitchen.dish.service.CookingStepImages.validate(images));
      var step = new DishRequest.CookingStepRequest(1, null, "cook", null, null, null, null, images);
      assertDoesNotThrow(() -> service().submit(1L, 2L, null, request(List.of(step))));
    }
  }
  @Test void legacyDeletionReorderAndMetadataChangesAreRejectedButCompleteModernArraysCanReorder() throws Exception {
    var first = existing(); var second = existing(); second.setStepNo(2); second.setContent("serve");
    when(dishes.selectCookingSteps(8L)).thenReturn(List.of(first, second));
    for (var steps : List.of(List.of(new DishRequest.CookingStepRequest(1, null, "cook")),
        List.of(new DishRequest.CookingStepRequest(1, null, "serve"), new DishRequest.CookingStepRequest(2, null, "cook")),
        List.of(new DishRequest.CookingStepRequest(1, null, "cook", 5, null, null, null), new DishRequest.CookingStepRequest(2, null, "serve")))) {
      assertThrows(BusinessException.class, () -> service().submit(1L, 2L, 8L, request(steps)));
    }
    assertDoesNotThrow(() -> service().submit(1L, 2L, 8L, request(List.of(
        new DishRequest.CookingStepRequest(1, null, "serve", null, null, null, null, second.getImageUrls()),
        new DishRequest.CookingStepRequest(2, null, "cook", null, null, null, null, first.getImageUrls())))));
  }
  @Test void ambiguousHistoricalApprovalRejectsBeforeDeletingAnyChildren() throws Exception {
    when(dishes.selectCookingSteps(8L)).thenReturn(List.of(existing()));
    var review = new com.familykitchen.dish.model.entity.DishReviewSubmissionDO();
    review.setMerchantId(2L); review.setTargetDishId(8L); review.setStatus("PENDING");
    review.setSnapshotJson(json.writeValueAsString(request(List.of(new DishRequest.CookingStepRequest(1, null, "changed")))));
    when(reviews.selectById(7L)).thenReturn(review);
    when(dishes.countCategoryOwnership(2L, 3L)).thenReturn(1);
    assertThrows(BusinessException.class, () -> service().approve(7L, 99L, "ok"));
    verify(dishes, never()).deleteCookingSteps(any());
    verify(dishes, never()).deleteDishIngredients(any());
    verify(dishes, never()).updateDish(any());
  }
  @Test void templateOmissionUsesIdentityNotPositionAndEmptyExplicitlyClears() {
    var first = new com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity();
    first.setItemKey("a"); first.setImageUrls(List.of("/uploads/images/a.jpg"));
    var second = new com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity();
    second.setItemKey("b"); second.setImageUrls(List.of("/uploads/images/b.jpg"));
    var rows = List.of(first, second);
    assertEquals(second.getImageUrls(), com.familykitchen.dish.service.CookingStepImages.resolveTemplate(1L, "b", null, rows));
    assertEquals(List.of(), com.familykitchen.dish.service.CookingStepImages.resolveTemplate(1L, "a", List.of(), rows));
    assertEquals(List.of(), com.familykitchen.dish.service.CookingStepImages.resolveTemplate(1L, "new", null, rows));
  }
  @Test void legacySubmissionResolvesImagesBeforeSavingSnapshot() throws Exception {
    when(dishes.selectCookingSteps(8L)).thenReturn(List.of(existing()));
    var result = service().submit(1L, 2L, 8L, request(List.of(new DishRequest.CookingStepRequest(1, null, "cook"))));
    assertEquals("/uploads/images/old.jpg", json.readTree(result.getSnapshotJson()).at("/cookingSteps/0/imageUrls/0").asText());
  }
  @Test void legacyTemplateClientsMatchDatabaseStepIdsWithoutLosingPhotos() {
    var step = new com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity();
    step.setId(123L); step.setItemKey("admin:8:step-original");
    step.setImageUrls(List.of("/uploads/images/kept.jpg"));
    for (String key : List.of("admin:8:step-original", "step-123", "template-step:123")) {
      assertEquals(step.getImageUrls(), com.familykitchen.dish.service.CookingStepImages.resolveTemplate(8L, key, null, List.of(step)), key);
    }
    assertEquals(List.of(), com.familykitchen.dish.service.CookingStepImages.resolveTemplate(8L, "step-124", null, List.of(step)));
  }
  @Test void legacyChangedStepsRejectBeforeMutation() throws Exception {
    when(dishes.selectCookingSteps(8L)).thenReturn(List.of(existing()));
    assertThrows(BusinessException.class, () -> service().submit(1L, 2L, 8L,
        request(List.of(new DishRequest.CookingStepRequest(1, null, "changed")))));
    verify(reviews, never()).insert(any());
    verify(dishes, never()).deleteCookingSteps(any());
  }
  @Test void submissionsRejectSixAndUnsafeImages() {
    for (List<String> images : List.of(java.util.Collections.nCopies(6, "/uploads/images/a.jpg"),
        List.of("javascript:alert(1)"), List.of("data:image/png;base64,abc"), List.of("/uploads/images/../x.jpg"),
        List.of("https://example.com/\nx.jpg"), List.of("http://example.com/a.jpg"))) {
      var step = new DishRequest.CookingStepRequest(1, null, "cook", null, null, null, null, images);
      assertThrows(BusinessException.class, () -> service().submit(1L, 2L, null, request(List.of(step))), images.toString());
    }
  }
  @Test void approvalPreservesLegacyImagesAndExplicitEmptyClears() throws Exception {
    for (List<String> images : java.util.Arrays.asList(null, List.<String>of())) {
      reset(dishes, reviews);
      when(dishes.selectCookingSteps(8L)).thenReturn(List.of(existing()));
      var review = new com.familykitchen.dish.model.entity.DishReviewSubmissionDO();
      review.setId(7L); review.setMerchantId(2L); review.setTargetDishId(8L); review.setStatus("PENDING");
      review.setSnapshotJson(json.writeValueAsString(request(List.of(
          new DishRequest.CookingStepRequest(1, null, "cook", null, null, null, null, images)))));
      when(reviews.selectById(7L)).thenReturn(review);
      when(reviews.approve(7L, 99L, "ok")).thenReturn(1);
      when(dishes.countCategoryOwnership(2L, 3L)).thenReturn(1);
      when(dishes.updateDish(any())).thenReturn(1);
      service().approve(7L, 99L, "ok");
      var saved = org.mockito.ArgumentCaptor.forClass(DishCookingStepEntity.class);
      verify(dishes).insertCookingStep(saved.capture());
      assertEquals(images == null ? 1 : 0, json.valueToTree(saved.getValue()).path("imageUrls").size());
    }
  }

  @Test void requestsRoundTripOrderedImagesAndDistinguishMissingFromEmpty() throws Exception {
    for (String images : List.of("[]", "[\"/uploads/images/a.jpg\",\"https://example.com/b.png\"]")) {
      var input = json.readTree("{\"stepNo\":1,\"content\":\"cook\",\"imageUrls\":" + images + "}");
      var request = json.treeToValue(input, DishRequest.CookingStepRequest.class);
      assertEquals(input.get("imageUrls"), json.valueToTree(request).get("imageUrls"));
    }
    assertTrue(json.valueToTree(new DishRequest.CookingStepRequest(1, null, "cook"))
        .has("imageUrls"));
    assertTrue(json.valueToTree(new DishRequest.CookingStepRequest(1, null, "cook"))
        .get("imageUrls").isNull());
  }
}
