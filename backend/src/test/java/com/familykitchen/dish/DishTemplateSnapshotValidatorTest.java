package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.dish.model.dto.DishTemplateSnapshotRequest;
import com.familykitchen.dish.service.DishTemplateSnapshotValidator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 验证模板菜品完整覆盖快照的严格解析、规范化和业务边界。 */
class DishTemplateSnapshotValidatorTest {

  private ObjectMapper objectMapper;
  private DishTemplateSnapshotValidator validator;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    validator = new DishTemplateSnapshotValidator(objectMapper);
  }

  @Test
  void acceptsAndNormalizesACompleteVersionOneSnapshot() {
    ObjectNode snapshot = validSnapshot();
    snapshot.put("name", "  豆角焖面  ");
    snapshot.withArray("tasteTags").add(" 家常 ").add("家常");

    DishTemplateSnapshotRequest result = validator.parseAndValidate(snapshot);

    assertEquals("豆角焖面", result.name());
    assertEquals(1, result.tasteTags().size());
    assertEquals("家常", result.tasteTags().get(0));
    assertEquals(new BigDecimal("18.00"), result.referencePrice());
  }

  @Test
  void rejectsUnknownFieldsAndUnsupportedSchemaVersions() {
    ObjectNode unknown = validSnapshot().put("templateCode", "FORBIDDEN");
    BusinessException unknownError = assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(unknown));
    assertTrue(unknownError.getMessage().contains("未知字段"));

    ObjectNode unsupported = validSnapshot().put("schemaVersion", 2);
    BusinessException versionError = assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(unsupported));
    assertEquals("模板菜品快照版本不支持", versionError.getMessage());
  }

  @Test
  void rejectsMissingFieldsAndNonLocalImages() {
    ObjectNode incomplete = validSnapshot();
    incomplete.remove("description");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(incomplete));

    ObjectNode externalImage = validSnapshot().put("imageUrl", "https://example.com/dish.jpg");
    BusinessException error = assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(externalImage));
    assertEquals("菜品图片必须使用本地上传地址", error.getMessage());
  }

  @Test
  void rejectsOversizedSnapshotUsingUtf8ByteLength() {
    ObjectNode oversized = validSnapshot();
    oversized.put("description", "菜".repeat(400_000));

    BusinessException error = assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(oversized));

    assertEquals("模板菜品目标快照不能超过1MB", error.getMessage());
  }

  @Test
  void rejectsInvalidMoneyTagsAndSortOrder() {
    assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(validSnapshot().put("referencePrice", 1.001)));
    assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(validSnapshot().put("sortOrder", 1_000_001)));

    ObjectNode invalidMeal = validSnapshot();
    invalidMeal.withArray("mealTags").add("BRUNCH");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(invalidMeal));
  }

  @Test
  void rejectsDuplicateIngredientsAndInvalidCalculationQuantity() {
    ObjectNode duplicate = validSnapshot();
    duplicate.withArray("ingredients").add(ingredient(" 豆角 ", "FIXED", "100.00"));
    BusinessException duplicateError = assertThrows(BusinessException.class,
        () -> validator.parseAndValidate(duplicate));
    assertEquals("食材名称不能重复", duplicateError.getMessage());

    ObjectNode noPurchase = validSnapshot();
    ((ObjectNode) noPurchase.withArray("ingredients").get(0))
        .put("calcType", "NO_PURCHASE").put("quantity", "1.00");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(noPurchase));

    ObjectNode fixed = validSnapshot();
    ((ObjectNode) fixed.withArray("ingredients").get(0)).put("quantity", "0.00");
    assertThrows(BusinessException.class, () -> validator.parseAndValidate(fixed));
  }

  private ObjectNode validSnapshot() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("schemaVersion", 1);
    node.put("categoryId", 1L);
    node.put("name", "豆角焖面");
    node.put("description", "北方家常焖面");
    node.put("imageUrl", "/images/dish-templates/dou-jiao-men-mian.jpg");
    node.put("imageSourceUrl", "https://example.com/source");
    node.put("imageAuthor", "家庭厨房");
    node.put("imageLicense", "授权使用");
    node.put("referencePrice", "18.00");
    node.putArray("tasteTags").add("家常");
    node.putArray("mealTags").add("LUNCH").add("DINNER");
    node.put("sortOrder", 10);
    node.put("enabled", true);
    node.putArray("ingredients").add(ingredient("豆角", "FIXED", "100.00"));
    return node;
  }

  private ObjectNode ingredient(String name, String calcType, String quantity) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("ingredientName", name);
    node.put("ingredientCategory", "蔬菜");
    node.put("quantity", quantity);
    node.put("unit", "克");
    node.put("calcType", calcType);
    node.put("sortOrder", 1);
    return node;
  }
}
