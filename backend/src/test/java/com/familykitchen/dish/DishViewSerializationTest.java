package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.familykitchen.dish.model.vo.DishView;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies that dish recommendation fields remain visible to JSON and OpenAPI clients. */
class DishViewSerializationTest {

  @Test
  void jacksonSerializesRecommendationFieldsAlongsideExistingDishFields() throws Exception {
    LocalDateTime featuredAt = LocalDateTime.of(2026, 8, 15, 9, 30);
    DishView view = new DishView(8L, 2L, "番茄炒蛋", "家常菜", "/dish.png",
        new BigDecimal("16.00"), "active", 4L, true, featuredAt, true);
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

    assertEquals(8L, json.get("dishId").longValue());
    assertEquals("番茄炒蛋", json.get("name").textValue());
    assertEquals("active", json.get("status").textValue());
    assertEquals(4L, json.get("sourceTemplateId").longValue());
    assertTrue(json.get("templateImported").booleanValue());
    assertTrue(json.get("featured").booleanValue());
    assertNotNull(json.get("featuredAt"));
  }

  @Test
  void reflectionAndOpenApiExposeBothRecommendationFieldsWithoutDroppingExistingFields() {
    Set<String> components = Arrays.stream(DishView.class.getRecordComponents())
        .map(component -> component.getName())
        .collect(Collectors.toSet());
    assertTrue(components.containsAll(Set.of(
        "dishId", "categoryId", "name", "description", "imageUrl", "price", "status",
        "sourceTemplateId", "templateImported", "featuredAt", "featured")));

    Schema<?> schema = ModelConverters.getInstance().read(DishView.class).get("DishView");
    assertNotNull(schema);
    assertTrue(schema.getProperties().keySet().containsAll(components));
  }
}
