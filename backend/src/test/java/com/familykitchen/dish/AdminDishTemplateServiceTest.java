package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.dto.AdminDishTemplateUpdateRequest;
import com.familykitchen.dish.model.dto.DishTemplateImageRejectionRequest;
import com.familykitchen.dish.model.entity.DishTemplateCategoryEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateImageAssetEntity;
import com.familykitchen.dish.service.DishTemplateImageStorageService;
import com.familykitchen.dish.service.DishTemplateProcurementReadinessEvaluator;
import com.familykitchen.dish.service.impl.AdminDishTemplateServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 验证平台模板编辑和图片审核的权限、并发与状态边界。 */
@ExtendWith(MockitoExtension.class)
class AdminDishTemplateServiceTest {
  @Test void legacyTemplateStepPreservesImagesByStableItemKey() {
    when(mapper.selectTemplateForUpdate(8L)).thenReturn(template(8L, 3L));
    when(mapper.selectCategoryForUpdate(2L)).thenReturn(category());
    when(evaluator.evaluate(any(), any(), any())).thenReturn(
        new DishTemplateProcurementReadinessEvaluator.EvaluationResult(true, List.of(), List.of()));
    when(mapper.updateAdminTemplate(any(), any())).thenReturn(1);
    var old = new com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity();
    old.setItemKey("step-1"); old.setImageUrls(List.of("/uploads/images/old.jpg"));
    when(mapper.selectTemplateCookingSteps(8L)).thenReturn(List.of(old));
    service.update(admin, 8L, request(3L));
    var saved = org.mockito.ArgumentCaptor.forClass(com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity.class);
    verify(mapper).insertTemplateCookingStep(saved.capture());
    assertEquals(old.getImageUrls(), saved.getValue().getImageUrls());
  }
  @Test void templateNourishmentPreservesNullClearsBlankAndSurvivesNormalSwitch() throws Exception {
    DishTemplateEntity existing = template(8L, 3L);
    existing.setProductType("NOURISHMENT"); existing.setNourishmentDescription("保留介绍"); existing.setServingAdvice("旧建议");
    when(mapper.selectTemplateForUpdate(8L)).thenReturn(existing);
    when(mapper.selectCategoryForUpdate(2L)).thenReturn(category());
    when(evaluator.evaluate(any(), any(), any())).thenReturn(
        new DishTemplateProcurementReadinessEvaluator.EvaluationResult(true, List.of(), List.of()));
    when(mapper.updateAdminTemplate(any(), any())).thenReturn(1);
    ObjectMapper json = new ObjectMapper();
    com.fasterxml.jackson.databind.node.ObjectNode body = json.valueToTree(request(3L));
    body.put("productType", "NORMAL").put("servingAdvice", "   ");
    service.update(admin, 8L, json.treeToValue(body, AdminDishTemplateUpdateRequest.class));
    assertEquals("NORMAL", existing.getProductType());
    assertEquals("保留介绍", existing.getNourishmentDescription());
    org.junit.jupiter.api.Assertions.assertNull(existing.getServingAdvice());
  }
  @Mock private DishTemplateMapper mapper;
  @Mock private DishTemplateProcurementReadinessEvaluator evaluator;
  @Mock private DishTemplateImageStorageService storage;

  private AdminDishTemplateServiceImpl service;
  private CurrentUserContext admin;

  @BeforeEach
  void setUp() {
    service = new AdminDishTemplateServiceImpl(mapper, evaluator, storage, new ObjectMapper());
    admin = new CurrentUserContext(1L, null, null, 1L, "admin",
        Set.of("PLATFORM_ADMIN"), Set.of(), "session");
  }

  @Test
  void updateRejectsStaleVersionBeforeReplacingChildren() {
    DishTemplateEntity template = template(8L, 3L);
    when(mapper.selectTemplateForUpdate(8L)).thenReturn(template);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.update(admin, 8L, request(2L)));

    assertEquals(ErrorCode.STATE_CONFLICT, error.errorCode());
    verify(mapper, never()).deleteTemplateIngredients(any());
    verify(mapper, never()).deleteTemplateCookingSteps(any());
  }

  @Test
  void updatePreservesServerOwnedImageAndSourceFields() {
    DishTemplateEntity template = template(8L, 3L);
    template.setImageUrl("/images/licensed.jpg");
    template.setImageAuthor("author");
    template.setImageLicense("CC0");
    template.setImageSourceUrl("https://source.example/image");
    template.setSourceKey("炒菜/示例.md");
    when(mapper.selectTemplateForUpdate(8L)).thenReturn(template);
    when(mapper.selectCategoryForUpdate(2L)).thenReturn(category());
    when(evaluator.evaluate(any(), any(), any())).thenReturn(
        new DishTemplateProcurementReadinessEvaluator.EvaluationResult(true, List.of(), List.of()));
    when(mapper.updateAdminTemplate(any(), any())).thenReturn(1);

    var result = service.update(admin, 8L, request(3L));

    assertEquals(4L, result.version());
    assertEquals("READY", result.dataStatus());
    verify(mapper).updateAdminTemplate(any(DishTemplateEntity.class), any(Long.class));
  }

  @Test
  void rejectedAssetCannotBePreviewed() {
    DishTemplateImageAssetEntity asset = asset("REJECTED");
    when(mapper.selectTemplateImageAsset(31L)).thenReturn(asset);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.preview(admin, 31L));

    assertEquals(ErrorCode.NOT_FOUND, error.errorCode());
    verify(storage, never()).preview(any());
  }

  @Test
  void updateRejectsServerOwnedImageField() {
    AdminDishTemplateUpdateRequest clean = request(3L);
    AdminDishTemplateUpdateRequest unsafe = new AdminDishTemplateUpdateRequest(clean.schemaVersion(),
        clean.expectedVersion(), clean.name(), clean.description(), clean.categoryId(),
        clean.referencePrice(), clean.tasteTags(), clean.mealTags(), clean.enabled(), clean.ingredients(),
        clean.cookingSteps(), "/uploads/forged.jpg", null, null, null, null, null, null, null,
        null, null, null, null, null, null);

    BusinessException error = assertThrows(BusinessException.class,
        () -> service.update(admin, 8L, unsafe));

    assertEquals(ErrorCode.BAD_REQUEST, error.errorCode());
    verify(mapper, never()).selectTemplateForUpdate(any());
  }

  @Test
  void rejectOnlyTransitionsInternalReviewAsset() {
    DishTemplateImageAssetEntity asset = asset("INTERNAL_REVIEW");
    when(mapper.selectTemplateImageAssetForUpdate(31L)).thenReturn(asset);
    when(mapper.rejectTemplateImageAsset(31L, 1L, "图片授权无法确认")).thenReturn(1);

    var result = service.rejectImage(admin, 8L, 31L,
        new DishTemplateImageRejectionRequest("图片授权无法确认"));

    assertEquals("REJECTED", result.assetStatus());
    verify(mapper).rejectTemplateImageAsset(31L, 1L, "图片授权无法确认");
  }

  private static DishTemplateEntity template(Long id, Long version) {
    DishTemplateEntity value = new DishTemplateEntity();
    value.setId(id); value.setVersion(version); value.setTemplateType("DISH");
    value.setSourceType("COOK_LIKE_HOC"); value.setCategoryId(1L);
    value.setSortOrder(1); value.setImageRightsStatus("DECLARED");
    return value;
  }

  private static DishTemplateCategoryEntity category() {
    DishTemplateCategoryEntity value = new DishTemplateCategoryEntity();
    value.setId(2L); value.setEnabled(true); return value;
  }

  private static DishTemplateImageAssetEntity asset(String status) {
    DishTemplateImageAssetEntity value = new DishTemplateImageAssetEntity();
    value.setId(31L); value.setTemplateId(8L); value.setAssetStatus(status);
    value.setInternalStorageKey("cooklikehoc/asset.jpg"); value.setMimeType("image/jpeg");
    value.setContentSha256("a".repeat(64)); return value;
  }

  private static AdminDishTemplateUpdateRequest request(Long version) {
    return new AdminDishTemplateUpdateRequest(2, version, "示例菜", "示例简介", 2L,
        new BigDecimal("18.00"), List.of("家常"), List.of("DINNER"), true,
        List.of(new AdminDishTemplateUpdateRequest.IngredientItem("line-1", "豆角", "蔬菜",
            "VERIFIED", new BigDecimal("200"), "g", "FIXED", "豆角 200g", null,
            null, null, 1)),
        List.of(new AdminDishTemplateUpdateRequest.CookingStepItem("step-1", 1, "焖制",
            "加入豆角焖熟", 600, null, "中火", null)));
  }
}
