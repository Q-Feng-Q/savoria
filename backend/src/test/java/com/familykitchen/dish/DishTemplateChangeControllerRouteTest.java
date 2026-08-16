package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.dish.controller.AdminDishTemplateChangeRequestController;
import com.familykitchen.dish.controller.DishTemplateChangeRequestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

/** 验证模板菜品修改审核的商户端与平台端路由和 springdoc 契约。 */
class DishTemplateChangeControllerRouteTest {

  @Test
  void merchantControllerExposesFourStableRoutes() {
    assertEquals("/merchant", basePath(DishTemplateChangeRequestController.class));
    assertMethods(DishTemplateChangeRequestController.class, "submit", "submitFromImportedDish",
        "page", "detail", "withdraw");
  }

  @Test
  void adminControllerExposesFourStableRoutes() {
    assertEquals("/admin/dish-template-change-requests",
        basePath(AdminDishTemplateChangeRequestController.class));
    assertMethods(AdminDishTemplateChangeRequestController.class, "page", "detail", "approve", "reject");
  }

  @Test
  void bothControllersDeclareBearerSecurityAndEveryRouteHasOperationDocumentation() {
    for (Class<?> type : ListHolder.CONTROLLERS) {
      SecurityRequirement security = type.getAnnotation(SecurityRequirement.class);
      assertNotNull(security);
      assertEquals("bearerAuth", security.name());
      Arrays.stream(type.getDeclaredMethods())
          .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
          .forEach(method -> assertNotNull(method.getAnnotation(Operation.class), method.getName()));
    }
  }

  private static String basePath(Class<?> type) {
    return type.getAnnotation(RequestMapping.class).value()[0];
  }

  private static void assertMethods(Class<?> type, String... names) {
    Set<String> actual = Arrays.stream(type.getDeclaredMethods())
        .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
        .map(Method::getName).collect(Collectors.toSet());
    assertTrue(actual.containsAll(Set.of(names)));
  }

  /** 保存参与路由契约检查的 Controller 类型。 */
  private static final class ListHolder {
    private static final Class<?>[] CONTROLLERS = {
        DishTemplateChangeRequestController.class, AdminDishTemplateChangeRequestController.class};
  }
}
