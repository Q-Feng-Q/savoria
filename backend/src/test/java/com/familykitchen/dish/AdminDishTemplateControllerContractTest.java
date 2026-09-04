package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.dish.controller.AdminDishTemplateController;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/** 固定平台模板管理端点，避免前后端联调期间发生静默路由漂移。 */
class AdminDishTemplateControllerContractTest {
  @Test
  void exposesListDetailUpdatePreviewPromotionAndRejectionRoutes() {
    assertRoute("page", GetMapping.class);
    assertRoute("detail", GetMapping.class);
    assertRoute("update", PutMapping.class);
    assertRoute("sourceRecords", GetMapping.class);
    assertRoute("preview", GetMapping.class);
    assertRoute("promoteImage", PostMapping.class);
    assertRoute("rejectImage", PostMapping.class);
  }

  private static void assertRoute(String name, Class<?> annotation) {
    Method method = Arrays.stream(AdminDishTemplateController.class.getDeclaredMethods())
        .filter(candidate -> candidate.getName().equals(name)).findFirst().orElse(null);
    assertNotNull(method, name + " route is missing");
    assertTrue(method.isAnnotationPresent(annotation.asSubclass(java.lang.annotation.Annotation.class)));
  }
}
