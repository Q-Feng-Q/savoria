package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.dish.model.dto.DishTemplateSnapshotRequest;
import com.familykitchen.dish.service.DishTemplateSnapshotValidator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 验证第二版审核快照的严格字段、可空数量和制作步骤规则。 */
class DishTemplateSnapshotValidatorTest {
  @Test void validatesOrderedStepImagesAndPreservesOmission() {
    ObjectNode snapshot = validSnapshot();
    ObjectNode step = (ObjectNode) snapshot.path("cookingSteps").get(0);
    assertNull(validator.parseAndValidate(snapshot).cookingSteps().get(0).imageUrls());
    step.putArray("imageUrls").add("/uploads/images/a.jpg").add("https://example.com/b.png");
    assertEquals(java.util.List.of("/uploads/images/a.jpg", "https://example.com/b.png"),
        validator.parseAndValidate(snapshot).cookingSteps().get(0).imageUrls());
    step.putArray("imageUrls").add("file:///tmp/a.jpg");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(snapshot));
    var images = step.putArray("imageUrls");
    for (int i = 0; i < 6; i++) images.add("/uploads/images/a.jpg");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(snapshot));
  }
  @Test void nourishmentPreservesOmissionAndExplicitClearing() {
    ObjectNode node = validSnapshot().put("productType", "NOURISHMENT")
        .put("nourishmentDescription", "  食品\n特点  ").put("servingAdvice", "   ");
    var result = validator.parseAndValidate(node);
    assertEquals("NOURISHMENT", result.productType());
    assertEquals("食品\n特点", result.nourishmentDescription());
    assertEquals("", result.servingAdvice());
    assertNull(result.precautions());
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(validSnapshot().put("productType", "")));
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(validSnapshot().put("precautions", "字".repeat(1001))));
  }
  private ObjectMapper objectMapper;
  private DishTemplateSnapshotValidator validator;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    validator = new DishTemplateSnapshotValidator(objectMapper);
  }

  @Test
  void acceptsCompleteVersionTwoSnapshotWithNullablePriceAndOrderedSteps() {
    ObjectNode snapshot = validSnapshot();
    snapshot.putNull("referencePrice");
    DishTemplateSnapshotRequest result = validator.parseAndValidate(snapshot);
    assertEquals(2, result.schemaVersion());
    assertNull(result.referencePrice());
    assertEquals("step-1", result.cookingSteps().get(0).itemId());
  }

  @Test
  void acceptsUploadedImageReferenceAndRightsConfirmation() {
    ObjectNode snapshot = validSnapshot();
    snapshot.put("imageUrl", "/uploads/images/dish.jpg");
    snapshot.put("imageAssetId", 19L);
    snapshot.put("imageRightsConfirmed", true);

    DishTemplateSnapshotRequest result = validator.parseAndValidate(snapshot);

    assertEquals("/uploads/images/dish.jpg", result.imageUrl());
    assertEquals(19L, result.imageAssetId());
    assertTrue(result.imageRightsConfirmed());
  }

  @Test
  void rejectsImageSourceAndDerivedServerFields() {
    for (String field : new String[] {"imageAuthor", "imageLicense", "sourceType", "dataStatus",
        "procurementReady", "templateType"}) {
      ObjectNode unsafe = validSnapshot().put(field, "forbidden");
      BusinessException error = assertThrows(BusinessException.class,
          () -> validator.parseAndValidate(unsafe));
      assertTrue(error.getMessage().contains("未知字段"), field);
    }
  }

  @Test
  void requiresRightsConfirmationForUploadedImage() {
    ObjectNode snapshot = validSnapshot();
    snapshot.put("imageUrl", "/uploads/images/dish.jpg");
    snapshot.put("imageAssetId", 19L);

    assertEquals("更换模板图片前必须确认拥有合法使用权",
        assertThrows(BusinessException.class, () -> validator.parseAndValidate(snapshot)).getMessage());
  }

  @Test
  void rejectsVersionOneAndNonContiguousSteps() {
    assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(validSnapshot().put("schemaVersion", 1)));
    ObjectNode invalid = validSnapshot();
    ((ObjectNode) invalid.withArray("cookingSteps").get(0)).put("stepNo", 2);
    assertEquals("制作步骤序号必须从1开始连续排列",
        assertThrows(BusinessException.class, () -> validator.parseAndValidate(invalid)).getMessage());
  }

  @Test
  void enforcesQuantityStateNullabilityAndStableItemIds() {
    ObjectNode missing = validSnapshot();
    ObjectNode row = (ObjectNode) missing.withArray("ingredients").get(0);
    row.put("quantityStatus", "MISSING"); row.putNull("quantity"); row.putNull("unit"); row.putNull("calcType");
    assertEquals("MISSING", validator.parseAndValidate(missing).ingredients().get(0).quantityStatus());

    ObjectNode invalid = validSnapshot();
    ((ObjectNode) invalid.withArray("ingredients").get(0)).put("quantityStatus", "MISSING");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(invalid));

    ObjectNode duplicate = validSnapshot();
    duplicate.withArray("ingredients").add(ingredient("line-1", "葱"));
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(duplicate));
  }

  @Test
  void rejectsOversizedUtf8Snapshot() {
    ObjectNode oversized = validSnapshot().put("description", "菜".repeat(400_000));
    assertEquals("模板菜品目标快照不能超过1MB",
        assertThrows(BusinessException.class, () -> validator.parseAndValidate(oversized)).getMessage());
  }

  private ObjectNode validSnapshot() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("schemaVersion", 2); node.put("categoryId", 1L); node.put("name", "豆角焖面");
    node.put("description", "北方家常焖面"); node.put("referencePrice", "18.00");
    node.putArray("tasteTags").add("家常"); node.putArray("mealTags").add("DINNER");
    node.put("sortOrder", 10); node.put("enabled", true);
    node.putArray("ingredients").add(ingredient("line-1", "豆角"));
    ObjectNode step = objectMapper.createObjectNode();
    step.put("itemId", "step-1"); step.put("stepNo", 1); step.put("title", "焖制");
    step.put("content", "加入豆角焖熟"); step.put("durationSeconds", 600); step.put("heatLevel", "中火");
    node.putArray("cookingSteps").add(step);
    return node;
  }

  private ObjectNode ingredient(String itemId, String name) {
    ObjectNode row = objectMapper.createObjectNode();
    row.put("itemId", itemId); row.put("ingredientName", name); row.put("ingredientCategory", "蔬菜");
    row.put("quantityStatus", "VERIFIED"); row.put("quantity", new BigDecimal("100.00"));
    row.put("unit", "g"); row.put("calcType", "FIXED"); row.put("sortOrder", 1);
    return row;
  }
}
